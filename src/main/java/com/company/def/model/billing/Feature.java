package com.company.def.model.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "features")
public class Feature {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "name")
    private String name;

    public Feature() {
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Feature)) {
            return false;
        }
        Feature feature = (Feature) o;
        return id == feature.id &&
            Objects.equals(name, feature.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }

    }
}
