package com.company.def.dto.stl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StlFile {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("filename")
    private final String filename;
    @JsonProperty("color")
    private final String color;

    public StlFile (final String name, final String filename) {
        this.name = name;
        this.filename = filename;
        this.color = null;
    }

    public StlFile (final String name, final String filename, final String color) {
        this.name = name;
        this.filename = filename;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlFile)) {
            return false;
        }
        StlFile stlFile = (StlFile) o;
        return Objects.equals(name, stlFile.name) &&
            Objects.equals(filename, stlFile.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filename);
    }

    @Override
    public String toString() {
        return "StlFile{" +
            "name='" + name + '\'' +
            ", filename='" + filename + '\'' +
            '}';
    }
}
