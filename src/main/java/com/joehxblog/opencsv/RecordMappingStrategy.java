package com.joehxblog.opencsv;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;


public class RecordMappingStrategy<T extends Record> extends HeaderColumnNameMappingStrategy<T> {

    public RecordMappingStrategy(Class<T> type) {
        this.setType(type);
    }

    @Override
    public T populateNewBean(String[] line) {
        var constructor = this.type.getConstructors()[0];
        var recordComponents = this.type.getRecordComponents();

        var initArgs = Stream.of(recordComponents)
                .map(recordComponent -> {
                    var index = this.headerIndex.getByName(recordComponent.getName())[0];
                    var cell = line[index];



                    if (cell.isBlank()) {
                        return null;
                    } else {
                        var type = recordComponent.getType();
                        var field = this.findField(index).getField();
                        var converter = this.determineConverter(field, type, null, null, null);

                        try {
                            return converter.convertToRead(cell);
                        } catch (CsvDataTypeMismatchException | CsvConstraintViolationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .toArray();

        T newInstance = null;
        try {
            newInstance = (T) constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return newInstance;
    }

}
