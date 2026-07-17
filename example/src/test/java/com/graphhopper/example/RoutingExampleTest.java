package com.graphhopper.example;

import com.graphhopper.util.Helper;
import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class RoutingExampleTest {

    @Test
    public void main() throws CsvValidationException, IOException {
        Helper.removeDir(new File("target/routing-graph-cache"));
        RoutingExample.main(new String[]{"../"});

        Helper.removeDir(new File("target/routing-tc-graph-cache"));
        RoutingExampleTC.main(new String[]{"../"});
    }
}
