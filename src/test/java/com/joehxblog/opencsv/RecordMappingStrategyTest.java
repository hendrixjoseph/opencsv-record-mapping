package com.joehxblog.opencsv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordMappingStrategyTest {

    public record TestRecord(String string, String multiWord, int integer) {}

    @ParameterizedTest
    @ValueSource(strings = {
            "string,multiWord,integer",
            "String,MultiWord,Integer",
            "String,Multi Word,Integer"
    })
    void testParse(String header) throws IOException {
        var stringReader = new StringReader(header + "\none,multi,1");

        var csvReader = new CSVReader(stringReader);

        var csvToBeanBuilder = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withType(TestRecord.class)
                .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class));

        var actualList = csvToBeanBuilder.build().parse();

        csvReader.close();

        var expectedList = List.of(new TestRecord("one", "multi", 1));

        assertEquals(expectedList, actualList);
    }

    @Test
    void testWrite() throws Exception {
        var stringWriter = new StringWriter();

        var list = List.of(
                new TestRecord("one", "multi", 1)
        );

        var csvWriter = new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
            .withQuotechar('\'')
            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
            .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class))
            .build();

        csvWriter.write(list);

        assertEquals("'integer','multiWord','string'\n'1','multi','one'\n", stringWriter.toString());
    }

    @Test
    void testWriteThenRead() throws Exception {
        var stringWriter = new StringWriter();

        var originalList = List.of(
                new TestRecord("one", "multi", 1)
        );

        var csvWriter = new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withMappingStrategy(new RecordMappingStrategy<>(TestRecord.class))
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

    @Test
    void testPrivateConstructorDoesNotWork() {
        record PrivateRecord(String string) {}

        assertThrows(RuntimeException.class, () -> new RecordMappingStrategy<>(PrivateRecord.class));
    }

    /**
     * This test shows that the traditional HeaderColumnNameMappingStrategy does not work
     * with records.
     * <p>
     * Should this test ever fail, then RecordMappingStrategy may no longer be necessary.
     */
    @DisplayName("Show that HeaderColumnNameMappingStrategy doesn't work.")
    @Test
    void testHeaderColumnNameMappingStrategy() {
        var stringReader = new StringReader("string,integer\none,1");

        var csvReader = new CSVReader(stringReader);

        var headerColumnNameMappingStrategy = new HeaderColumnNameMappingStrategy<TestRecord>();
        headerColumnNameMappingStrategy.setType(TestRecord.class);

        var csvToBeanBuilder = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withType(TestRecord.class)
                .withMappingStrategy(headerColumnNameMappingStrategy);

        assertThrows(RuntimeException.class, () -> csvToBeanBuilder.build().parse());
    }
}