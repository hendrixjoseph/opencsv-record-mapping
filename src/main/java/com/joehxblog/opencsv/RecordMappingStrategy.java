package com.joehxblog.opencsv;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.collections4.ListValuedMap;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Objects;
import java.util.stream.Stream;


public class RecordMappingStrategy<T extends Record> extends HeaderColumnNameMappingStrategy<T> {

    public RecordMappingStrategy(Class<T> type) {
        if (type.getConstructors().length > 0) {
            this.setType(type);
        } else {
            throw new RuntimeException("record needs to have its constructor be public");
        }
    }

    @Override
    public T populateNewBean(String[] line) {
        var constructor = this.type.getConstructors()[0];
        var recordComponents = this.type.getRecordComponents();

        var initArgs = Stream.of(recordComponents)
                .map(recordComponent -> this.createParameter(line, recordComponent))
                .toArray();

        try {
            return (T) constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void loadUnadornedFieldMap(ListValuedMap<Class<?>, Field> fields) {
        fields.entries().stream()
                .filter(entry -> !(Serializable.class.isAssignableFrom(entry.getKey()) && "serialVersionUID".equals(entry.getValue().getName())))
                .filter(entry -> !entry.getValue().isAnnotationPresent(CsvRecurse.class))
                .forEach(entry -> {
                    var converter = determineConverter(entry.getValue(), entry.getValue().getType(), null, null, null);
                    fieldMap.put(entry.getValue().getName(), new BeanFieldSingleValue<>(
                            entry.getKey(), entry.getValue(),
                            false, errorLocale, converter, null, null));
                });
    }

    @Override
    protected BeanField<T, String> findField(int col) throws CsvBadConverterException {
        BeanField<T, String> beanField = null;

        var columnName = getColumnName(col);

        if (columnName == null) {
            return null;
        }

        final var finalColumnName = columnName.trim().replace(" ", "");

        if (!columnName.isEmpty()) {
            beanField = Objects.requireNonNullElseGet(
                    fieldMap.get(finalColumnName),
                    () -> fieldMap.values().stream()
                    .filter(b -> b.getField().getName().equalsIgnoreCase(finalColumnName))
                    .findFirst()
                    .orElseThrow());
        }

        return beanField;
    }

    private Object createParameter(String[] line, RecordComponent recordComponent) {
        var index = getIndex(recordComponent);
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
    }

    private int getIndex(RecordComponent recordComponent) {
        var indexArray = this.headerIndex.getByName(recordComponent.getName());

        if (indexArray.length > 0) {
            return indexArray[0];
        } else {
            var headers = this.headerIndex.getHeaderIndex();
            var field = recordComponent.getName();
            for (int i = 0; i < headers.length; i++) {
                var header = headers[i].replace(" ", "");

                if (header.equalsIgnoreCase(field)) {
                    return i;
                }
            }
        }

        throw new RuntimeException("no header found for " + recordComponent.getName());
    }
}
