package com.joehxblog.opencsv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import com.opencsv.exceptions.CsvRuntimeException;

public class AnnotatedRecordMappingStrategy<T extends Record> extends HeaderColumnNameMappingStrategy<T> {

    public AnnotatedRecordMappingStrategy(Class<T> type) {
        setType(type);
    }

    @Override
    public T populateNewBean(String[] line) throws CsvBeanIntrospectionException, CsvFieldAssignmentException, CsvChainedException {
        var recordComponents = type.getRecordComponents();

        if (recordComponents.length != line.length) {
            throw new CsvRuntimeException("Mismatch between line values and record components");
        }

        var valuesByRecordComponentName = createValuesMap(line);
        var initArgs = Stream.of(recordComponents)
                .map(recordComponent -> valuesByRecordComponentName.get(recordComponent.getName()))
                .toArray();

        try {
            var array = Arrays.stream(recordComponents).map(RecordComponent::getType).toArray(Class[]::new);
            return type.getConstructor(array).newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new CsvRuntimeException("Error creating instance of record", e);
        }
    }

    private Map<String, Object> createValuesMap(String[] line) throws CsvConstraintViolationException, CsvDataTypeMismatchException {
        var valuesByRecordComponentName = new HashMap<String, Object>();

        for (int i = 0; i < line.length; i++) {
            var field = findField(i).getField();
            valuesByRecordComponentName.put(
                    field.getName(),
                    determineConverter(field, field.getType(), null, null, null).convertToRead(line[i]));
        }

        return valuesByRecordComponentName;
    }
}
