package com.joehxblog.opencsv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordMappingStrategyTest {

    public record TestRecord(String string, int integer) {};

    @Test
    void testParse() throws IOException {
        var stringReader = new StringReader("string,integer\none,1");

        var csvReader = new CSVReader(stringReader);

        var csvToBeanBuilder = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withType(TestRecord.class)
                .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class));

        var actualList = csvToBeanBuilder.build().parse();

        csvReader.close();

        var expectedList = List.of(new TestRecord("one", 1));

        assertEquals(expectedList, actualList);
    }

    @Test
    void testWrite() throws Exception {
        var stringWriter = new StringWriter();

        var list = List.of(
                new TestRecord("one", 1)
        );

        var csvWriter = new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
            .withQuotechar('\'')
            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
            .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class))
            .build();

        csvWriter.write(list);

        assertEquals("'INTEGER','STRING'\n'1','one'\n", stringWriter.toString());
    }

    @Test
    void testWriteThenRead() throws Exception {
        var stringWriter = new StringWriter();

        var originalList = List.of(
                new TestRecord("one", 1)
        );

        var csvWriter = new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
                .withQuotechar('\'')
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .build();

        csvWriter.write(originalList);

        var string = stringWriter.toString();

        var stringReader = new StringReader(string);

        var csvReader = new CSVReader(stringReader);

        var csvToBeanBuilder = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withType(TestRecord.class)
                .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class));

        var actualList = csvToBeanBuilder.build().parse();

        csvReader.close();

        assertEquals(originalList, actualList);

    }
}