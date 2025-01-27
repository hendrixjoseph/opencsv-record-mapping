package com.joehxblog.opencsv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

class AnnotatedRecordMappingStrategyTest {

    public record TestRecord(@CsvBindByName(column = "my_str") String string,
                             @CsvBindByName(column = "my_int") int integer) {}

    @Test
    void testParse() throws IOException {
        StringReader stringReader = new StringReader("my_int,my_str\n123,hello_world");
        CSVReader csvReader = new CSVReader(stringReader);

        List<TestRecord> actualList = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withMappingStrategy(new AnnotatedRecordMappingStrategy<>(TestRecord.class))
                .build()
                .parse();

        csvReader.close();

        List<TestRecord> expectedList = List.of(new TestRecord("hello_world", 123));
        assertEquals(expectedList, actualList);
    }

    @Test
    void testWrite() throws Exception {
        StringWriter stringWriter = new StringWriter();

        new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
                .withQuotechar('\'')
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withMappingStrategy(new AnnotatedRecordMappingStrategy<>(TestRecord.class))
                .build()
                .write(List.of(new TestRecord("one", 1)));

        assertEquals("'MY_INT','MY_STR'\n'1','one'\n", stringWriter.toString());
    }

    @Test
    void testWriteThenRead() throws Exception {
        // Read
        StringWriter stringWriter = new StringWriter();
        StatefulBeanToCsv<TestRecord> csvWriter = new StatefulBeanToCsvBuilder<TestRecord>(stringWriter)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withMappingStrategy(new AnnotatedRecordMappingStrategy<>(TestRecord.class))
                .build();

        List<TestRecord> originalList = List.of(new TestRecord("one", 1));
        csvWriter.write(originalList);

        // Write
        CSVReader csvReader = new CSVReader(new StringReader(stringWriter.toString()));
        CsvToBeanBuilder<TestRecord> csvToBeanBuilder = new CsvToBeanBuilder<TestRecord>(csvReader)
                .withType(TestRecord.class)
                .withMappingStrategy(new AnnotatedRecordMappingStrategy<>(TestRecord.class));

        List<TestRecord> actualList = csvToBeanBuilder.build().parse();
        assertEquals(originalList, actualList);

        csvReader.close();
    }
}
