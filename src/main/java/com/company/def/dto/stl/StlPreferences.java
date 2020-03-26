package com.cephx.def.dto.stl;

import java.util.Arrays;
import java.util.Objects;

public class StlPreferences {

    private String otherTeethColor = "#FF0000";
    private int[] teethNumbersInGroups;

    public StlPreferences() {
    }

    public String getOtherTeethColor() {
        return otherTeethColor;
    }

    public StlPreferences setOtherTeethColor(final String otherTeethColor) {
        this.otherTeethColor = otherTeethColor;
        return this;
    }

    public int[] getTeethNumbersInGroups() {
        return teethNumbersInGroups;
    }

    public StlPreferences setTeethNumbersInGroups(final int[] teethNumbersInGroups) {
        this.teethNumbersInGroups = teethNumbersInGroups;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlPreferences)) {
            return false;
        }
        StlPreferences that = (StlPreferences) o;
        return Objects.equals(otherTeethColor, that.otherTeethColor) &&
            Arrays.equals(teethNumbersInGroups, that.teethNumbersInGroups);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(otherTeethColor);
        result = 31 * result + Arrays.hashCode(teethNumbersInGroups);
        return result;
    }

    @Override
    public String toString() {
        return "StlPreferences{" +
            "otherTeethColor='" + otherTeethColor + '\'' +
            ", teethNumbersInGroups=" + Arrays.toString(teethNumbersInGroups) +
            '}';
    }
}
