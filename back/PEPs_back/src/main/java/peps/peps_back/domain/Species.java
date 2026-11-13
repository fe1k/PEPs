package peps.peps_back.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an animal species managed within the zoological enrichment platform
 * (e.g., Macaws, Saimiris, Dolphins) and maps to the {@code peps.species} table.
 *
 * @author Franco Valdebenito
 * @version 1.1
 */
@Entity
@Table(name = "species")
public class Species implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the species.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_species")
    private Integer speciesId;

    /**
     * Unique display name of the species (maximum 50 characters).
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Detailed description of the species and its enrichment context.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public Species() {
    }

    /**
     * Constructs a new {@code Species} instance with the specified name and description.
     *
     * @param name        the unique name of the species; must not be {@code null}
     * @param description an optional textual description of the species
     */
    public Species(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the unique identifier of this species.
     *
     * @return the primary key {@code speciesId}, or {@code null} if not yet persisted
     */
    public Integer getSpeciesId() {
        return speciesId;
    }

    /**
     * Sets the unique identifier of this species.
     *
     * @param speciesId the primary key to assign
     */
    public void setSpeciesId(Integer speciesId) {
        this.speciesId = speciesId;
    }

    /**
     * Returns the unique name of this species.
     *
     * @return the species name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name of this species.
     *
     * @param name the species name to set (maximum 50 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the textual description of this species.
     *
     * @return the species description, or {@code null} if none is provided
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the textual description of this species.
     *
     * @param description the description text to assign
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Compares this species to another object for equality based on {@code speciesId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same species; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Species species = (Species) o;
        return Objects.equals(speciesId, species.speciesId);
    }

    /**
     * Returns a hash code value for this species based on {@code speciesId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(speciesId);
    }

    /**
     * Returns a string representation of this {@code Species} entity.
     *
     * @return a formatted string containing the species ID and name
     */
    @Override
    public String toString() {
        return "Species{" +
                "speciesId=" + speciesId +
                ", name='" + name + '\'' +
                '}';
    }
}