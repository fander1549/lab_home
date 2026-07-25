package com.graphhopper.example;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates every routeable trip of one calendar day for the IRV perturbation
 * experiment.  All records receive a CH-shortest path; a deterministic,
 * nested 20% subset also receives the least-time different alternative that
 * stays within a configurable detour cap.  Thus 5%, 10%, 15%, and 20%
 * perturbations can all be produced without sampling any trips.
 *
 * <p>Arguments: {@code <nyc|chicago> <nyc-route-file-or--> <chicago-flow-file-or-->
 * <output-jsonl> <YYYY-MM-DD> <seed> [start-index batch-size rank-start alternative-quota]}</p>
 */
public final class IrvFullDayRouteGenerator {
    private static final DateTimeFormatter NYC_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CHICAGO_TIME = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US);
    private static final double MAX_ALTERNATIVE_FRACTION = 0.20;
    private static final double MAX_DETOUR_RATIO = Double.parseDouble(System.getProperty("irv.max_detour_ratio", "1.15"));

    private IrvFullDayRouteGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (MAX_DETOUR_RATIO <= 1.002) {
            throw new IllegalArgumentException("irv.max_detour_ratio must exceed 1.002");
        }
        if (args.length != 6 && args.length != 10) {
            throw new IllegalArgumentException("usage: <nyc|chicago> <nyc-route-file-or--> <chicago-flow-file-or--> <output-jsonl> <YYYY-MM-DD> <seed> [start-index batch-size rank-start alternative-quota]");
        }
        City city = City.valueOf(args[0].toUpperCase(Locale.ROOT));
        Path nycRouteFile = "-".equals(args[1]) ? null : Path.of(args[1]);
        Path chicagoFlowFile = "-".equals(args[2]) ? null : Path.of(args[2]);
        Path output = Path.of(args[3]);
        LocalDate day = LocalDate.parse(args[4]);
        long seed = Long.parseLong(args[5]);

        List<Candidate> candidates = city == City.NYC
                ? readNycDay(nycRouteFile, day)
                : readChicagoDay(chicagoFlowFile, day);
        if (candidates.isEmpty()) throw new IllegalArgumentException("No valid " + city + " trips on " + day);

        Collections.shuffle(candidates, new Random(seed));
        int requestedAlternatives = (int) Math.round(candidates.size() * MAX_ALTERNATIVE_FRACTION);
        int startIndex = args.length == 10 ? Integer.parseInt(args[6]) : 0;
        int batchSize = args.length == 10 ? Integer.parseInt(args[7]) : candidates.size();
        int rankStart = args.length == 10 ? Integer.parseInt(args[8]) : 0;
        int alternativeQuota = args.length == 10 ? Integer.parseInt(args[9]) : requestedAlternatives;
        if (startIndex < 0 || batchSize <= 0 || startIndex >= candidates.size()) {
            throw new IllegalArgumentException("Invalid chunk: start-index=" + startIndex + ", batch-size=" + batchSize + ", total=" + candidates.size());
        }
        int endIndex = Math.min(startIndex + batchSize, candidates.size());
        System.out.printf(Locale.ROOT, "%s %s: %d valid trips; generating %d alternative paths%n",
                city, day, candidates.size(), requestedAlternatives);

        GraphHopper hopper = createHopper(city);
        int written = 0;
        int baseFailures = 0;
        int alternativesInChunk = 0;
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter writer = Files.newBufferedWriter(output)) {
                for (int candidateIndex = startIndex; candidateIndex < endIndex; candidateIndex++) {
                    Candidate candidate = candidates.get(candidateIndex);
                    ResponsePath shortest = route(hopper, candidate, false);
                    if (shortest == null) {
                        baseFailures++;
                        continue;
                    }
                    ResponsePath alternative = null;
                    Integer rank = null;
                    // Try further candidates until exactly 20% have usable alternatives.
                    if (alternativesInChunk < alternativeQuota) {
                        ResponsePath candidateAlternative = route(hopper, candidate, true, shortest);
                        if (candidateAlternative != null) {
                            alternative = candidateAlternative;
                            rank = rankStart + alternativesInChunk;
                            alternativesInChunk++;
                        }
                    }
                    writeJson(writer, city, candidateIndex, candidate, shortest, alternative, rank);
                    written++;
                }
            }
        } finally {
            hopper.close();
        }
        System.out.printf(Locale.ROOT, "Wrote chunk [%d, %d): %d daily CH paths (%d base failures, %d alternatives) to %s%n",
                startIndex, endIndex, written, baseFailures, alternativesInChunk, output);
    }

    private static GraphHopper createHopper(City city) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(city == City.NYC ? "NewYork2.osm.pbf" : "Chicago.osm.pbf");
        // Reuse the prepared CH graphs created by the sample runner when present.
        hopper.setGraphHopperLocation("target/irv-perturbation-" + city.name().toLowerCase(Locale.ROOT));
        hopper.setProfiles(new Profile("car").setVehicle("car").setTurnCosts(false));
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
        hopper.importOrLoad();
        return hopper;
    }

    private static ResponsePath route(GraphHopper hopper, Candidate candidate, boolean alternative) {
        return route(hopper, candidate, alternative, null);
    }

    private static ResponsePath route(GraphHopper hopper, Candidate candidate, boolean alternative, ResponsePath shortest) {
        GHRequest request = new GHRequest(candidate.fromLat, candidate.fromLon, candidate.toLat, candidate.toLon)
                .setProfile("car")
                .setLocale(Locale.US);
        if (alternative) {
            // ALT_ROUTE is a flexible-mode GraphHopper algorithm.  CH remains
            // enabled for every baseline path and is disabled only here.
            request.setAlgorithm(Parameters.Algorithms.ALT_ROUTE);
            request.putHint(Parameters.CH.DISABLE, true);
            request.putHint(Parameters.Algorithms.AltRoute.MAX_PATHS, 3);
        }
        GHResponse response = hopper.route(request);
        if (response.hasErrors() || response.getAll().isEmpty()) return null;
        if (!alternative) return response.getBest();

        return response.getAll().stream()
                .filter(path -> isMeaningfullyDifferent(shortest, path))
                .filter(path -> path.getDistance() <= shortest.getDistance() * MAX_DETOUR_RATIO)
                .min(Comparator.comparingLong(ResponsePath::getTime)
                        .thenComparingDouble(ResponsePath::getDistance))
                .orElse(null);
    }

    private static boolean isMeaningfullyDifferent(ResponsePath shortest, ResponsePath alternative) {
        if (alternative == null || alternative.getDistance() <= shortest.getDistance() * 1.002) return false;
        PointList first = shortest.getPoints();
        PointList second = alternative.getPoints();
        if (first.size() < 2 || second.size() < 2) return false;
        return first.size() != second.size() || first.getLat(1) != second.getLat(1) || first.getLon(1) != second.getLon(1);
    }

    private static List<Candidate> readNycDay(Path routeFile, LocalDate day) throws IOException {
        if (routeFile == null) throw new IllegalArgumentException("NYC requires the precomputed route file");
        List<Candidate> candidates = new ArrayList<>();
        boolean seenDay = false;
        try (BufferedReader reader = Files.newBufferedReader(routeFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Candidate candidate = parseNyc(line, day);
                if (candidate != null) {
                    candidates.add(candidate);
                    seenDay = true;
                } else if (seenDay && line.contains(" _")) {
                    // Source files are chronological.  Avoid reading the rest of a 1+ GB monthly file.
                    int marker = line.lastIndexOf(" _");
                    if (marker >= 0 && line.substring(marker + 2).compareTo(day.plusDays(1).toString()) >= 0) break;
                }
            }
        }
        return candidates;
    }

    private static Candidate parseNyc(String line, LocalDate day) {
        int marker = line.lastIndexOf(" _");
        if (marker < 0) return null;
        String[] metadata = line.substring(marker + 2).split(",");
        if (metadata.length < 5 || !metadata[0].startsWith(day.toString())) return null;
        try {
            LocalDateTime start = LocalDateTime.parse(metadata[0].trim(), NYC_TIME);
            LocalDateTime end = LocalDateTime.parse(metadata[1].trim(), NYC_TIME);
            int durationSeconds = Math.toIntExact(Duration.between(start, end).getSeconds());
            double miles = Double.parseDouble(metadata[2].trim());
            double speedKmh = miles * 1.609 / (durationSeconds / 3600.0);
            // Same data-quality criteria used by the supplied NYC C# processor.
            if (miles <= 0 || miles >= 12 || durationSeconds < 30 || durationSeconds > 200 * 60 || speedKmh > 65) return null;

            String[] points = line.substring(0, marker).trim().split("\\s+");
            if (points.length < 6) return null;
            double fromLat = Double.parseDouble(points[0]);
            double fromLon = Double.parseDouble(points[1]);
            double toLat = Double.parseDouble(points[points.length - 3]);
            double toLon = Double.parseDouble(points[points.length - 2]);
            return candidate(fromLat, fromLon, toLat, toLon, durationSeconds, day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static List<Candidate> readChicagoDay(Path flowFile, LocalDate day) throws IOException {
        if (flowFile == null) throw new IllegalArgumentException("Chicago requires taxi_trip1-12flow_data.txt");
        List<Candidate> candidates = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(flowFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Candidate candidate = parseChicago(line, day);
                if (candidate != null) candidates.add(candidate);
            }
        }
        return candidates;
    }

    private static Candidate parseChicago(String line, LocalDate day) {
        String[] fields = line.split(",");
        if (fields.length < 6) return null;
        try {
            LocalDateTime start = LocalDateTime.parse(fields[3].trim(), CHICAGO_TIME);
            if (!start.toLocalDate().equals(day)) return null;
            LocalDateTime end = LocalDateTime.parse(fields[4].trim(), CHICAGO_TIME);
            int durationSeconds = Math.round(Float.parseFloat(fields[5].trim()));
            if (durationSeconds <= 0) durationSeconds = Math.toIntExact(Duration.between(start, end).getSeconds());
            if (durationSeconds <= 0 || durationSeconds > 4 * 60 * 60) return null;
            String[] coordinates = fields[2].trim().split("\\s+");
            if (coordinates.length != 4) return null;
            return candidate(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                    Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]), durationSeconds, day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Candidate candidate(double fromLat, double fromLon, double toLat, double toLon, int durationSeconds, LocalDate day) {
        if (!Double.isFinite(fromLat) || !Double.isFinite(fromLon) || !Double.isFinite(toLat) || !Double.isFinite(toLon)) return null;
        return new Candidate(fromLat, fromLon, toLat, toLon, durationSeconds, day);
    }

    private static void writeJson(BufferedWriter writer, City city, int tripId, Candidate candidate,
                                  ResponsePath shortest, ResponsePath alternative, Integer replacementRank) throws IOException {
        writer.write("{\"city\":\"");
        writer.write(city.name().toLowerCase(Locale.ROOT));
        writer.write("\",\"trip_id\":");
        writer.write(Integer.toString(tripId));
        writer.write(",\"date\":\"");
        writer.write(candidate.date.toString());
        writer.write("\",\"duration_s\":");
        writer.write(Integer.toString(candidate.durationSeconds));
        writer.write(",\"replacement_rank\":");
        writer.write(replacementRank == null ? "null" : replacementRank.toString());
        writer.write(",\"shortest_distance_m\":");
        writer.write(Double.toString(shortest.getDistance()));
        writer.write(",\"shortest\":");
        writePoints(writer, shortest.getPoints());
        writer.write(",\"alternative\":");
        if (alternative == null) writer.write("null");
        else writePoints(writer, alternative.getPoints());
        writer.write("}\n");
    }

    private static void writePoints(BufferedWriter writer, PointList points) throws IOException {
        writer.write('[');
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) writer.write(',');
            writer.write('[');
            writer.write(Double.toString(points.getLat(i)));
            writer.write(',');
            writer.write(Double.toString(points.getLon(i)));
            writer.write(']');
        }
        writer.write(']');
    }

    private enum City {NYC, CHICAGO}

    private record Candidate(double fromLat, double fromLon, double toLat, double toLon, int durationSeconds, LocalDate date) {
    }
}
