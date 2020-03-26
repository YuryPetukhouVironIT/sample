package com.cephx.def.dto.stl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class StlToothGroup {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("teeth")
    private final List<StlFile> teeth;

    public StlToothGroup(final String name, final List<StlFile> teeth) {
        this.name = name;
        this.teeth = teeth;
    }

    public String getName() {
        return name;
    }

    public List<StlFile> getTeeth() {
        return teeth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlToothGroup)) {
            return false;
        }
        StlToothGroup that = (StlToothGroup) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(teeth, that.teeth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, teeth);
    }

    @Override
    public String toString() {
        return "StlToothGroup{" +
            "name='" + name + '\'' +
            ", teeth=" + teeth +
            '}';
    }
}
