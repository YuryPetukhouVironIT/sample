package com.cephx.def.dto.stl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StlDentation {

    private final String name;
    private final List<StlToothGroup> groups;

    public StlDentation(final String name, final List<StlToothGroup> groups, final List<StlFile> files) {
        this.name = name;
        this.groups = CollectionUtils.isEmpty(groups) ? new ArrayList<StlToothGroup>() : groups;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("groups")
    public List<StlToothGroup> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlDentation)) {
            return false;
        }
        StlDentation that = (StlDentation) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groups);
    }

    @Override
    public String toString() {
        return "StlDentation{" +
            "name='" + name + '\'' +
            ", groups=" + groups +
            '}';
    }
}
