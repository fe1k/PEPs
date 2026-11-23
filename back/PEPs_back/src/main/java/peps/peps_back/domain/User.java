package peps.peps_back.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * Represents an authenticated platform user (e.g., administrator, caretaker, researcher)
 * with role-based and species-level access control, mapping to the {@code users} table.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer userId;

    /**
     * Unique authentication username (maximum 50 characters).
     */
    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    /**
     * Cryptographically hashed password (e.g., BCrypt).
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Functional security role assigned to the user (e.g., ROLE_ADMIN, ROLE_SOIGNEUR, ROLE_CHERCHEUR).
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * Flag indicating whether the user account is active or soft-disabled.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Timestamp indicating when the user account was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating the last modification of the user account.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set of animal species this user is authorized to supervise and consult,
     * mapped via the {@code user_species} join table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_species",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_species")
    )
    private Set<Species> supervisedSpecies = new HashSet<>();

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public User() {
    }

    /**
     * Constructs a new {@code User} instance with essential authentication and role attributes.
     *
     * @param login        the unique login username; must not be {@code null}
     * @param passwordHash the hashed password; must not be {@code null}
     * @param role         the functional role of the user (e.g., {@code "ROLE_SOIGNEUR"})
     */
    public User(String login, String passwordHash, String role) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
    }

    /**
     * Automatically initializes {@code createdAt} and {@code updatedAt} timestamps
     * before the entity is persisted for the first time.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    /**
     * Automatically updates the {@code updatedAt} timestamp before any database update.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the unique identifier of this user.
     *
     * @return the primary key {@code userId}, or {@code null} if not yet persisted
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier of this user.
     *
     * @param userId the primary key to assign
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * Returns the unique login username.
     *
     * @return the user login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the unique login username.
     *
     * @param login the login username to assign (maximum 50 characters)
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Returns the hashed password of this user.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed password of this user.
     *
     * @param passwordHash the hashed password to assign
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the functional role of this user.
     *
     * @return the role name
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the functional role of this user.
     *
     * @param role the role name to assign
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Returns whether this user account is active.
     *
     * @return {@code true} if active; {@code false} if disabled
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the active status of this user account.
     *
     * @param isActive {@code true} to enable the account; {@code false} to disable it
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Returns the account creation timestamp.
     *
     * @return the creation {@code LocalDateTime}
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the account creation timestamp.
     *
     * @param createdAt the creation {@code LocalDateTime} to assign
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the last account update timestamp.
     *
     * @return the last update {@code LocalDateTime}
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last account update timestamp.
     *
     * @param updatedAt the update {@code LocalDateTime} to assign
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the set of species supervised by this user.
     *
     * @return a {@code Set} of authorized {@code Species} entities
     */
    public Set<Species> getSupervisedSpecies() {
        return supervisedSpecies;
    }

    /**
     * Sets the collection of species supervised by this user.
     *
     * @param supervisedSpecies the {@code Set} of {@code Species} entities to assign
     */
    public void setSupervisedSpecies(Set<Species> supervisedSpecies) {
        this.supervisedSpecies = supervisedSpecies;
    }

    /**
     * Compares this user to another object for equality based on {@code userId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted user; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Returns a hash code value for this user based on {@code userId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * Returns a string representation of this {@code User} entity without exposing sensitive credentials.
     *
     * @return a formatted string containing the user ID, login, role, and active status
     */
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", login='" + login + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}