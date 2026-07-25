package com.learning.identity_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Stores the BCrypt HASH, never plaintext. We'll wire the actual
     * encoding in the Security phase — for now, just know this column
     * holds a hash, not a password.
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;


    /**
     * This is the OWNING side of the relationship — it has the
     * @JoinTable, which means Hibernate looks HERE to decide what to
     * write to the join table. If you add a role via user.getRoles().add(role)
     * and save the User, the join row gets written. If you instead did
     * role.getUsers().add(user) and saved the Role, NOTHING would
     * happen to the join table — that's the inverse side, remember.
     *
     * FetchType.EAGER here (overriding ManyToMany's LAZY default) is a
     * deliberate, justified choice: roles are small, always needed for
     * authorization checks on every authenticated request, and there
     * are only ever 2-3 per user. This is one of the few places EAGER
     * is genuinely the right call rather than a lazy-loading footgun.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles" , // the join table itself
            joinColumns = @JoinColumn(name = "user_id"), // FK column pointing back to User
            inverseJoinColumns = @JoinColumn(name = "role_id") // FK column pointing to Role
    )
    private Set<Role> roles = new HashSet<>();

    public User(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    /**
     * Helper methods that keep BOTH sides of the in-memory object graph
     * consistent when you add/remove a role. This doesn't affect what
     * gets persisted (that's still governed purely by the owning side),
     * but it prevents bugs where, within the same transaction/session,
     * user.getRoles() and role.getUsers() disagree with each other.
     * This pattern is worth using on every bidirectional relationship
     * in this project.
     */
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role){
        this.roles.remove(role);
        role.getUsers().remove(this);
    }


    /**
     * Inverse side of the relationship — mappedBy = "user" points at the
     * field name on Address that owns the FK. No @JoinColumn here; Hibernate
     * already knows where the FK lives from the owning side.
     *
     * cascade = CascadeType.ALL means: any persist/merge/remove/refresh/detach
     * operation performed on a User automatically cascades to every Address
     * in this collection. Concretely — save a new User with addresses already
     * added to this list, and you do NOT need to save each Address separately;
     * Hibernate persists them as part of saving the User. Delete the User, and
     * all their addresses get deleted too.
     *
     * orphanRemoval = true is a DIFFERENT, stronger guarantee than cascade
     * REMOVE. It means: if an Address is simply REMOVED FROM THIS LIST (even
     * without deleting the User at all), Hibernate deletes that Address row
     * from the database on next flush. Without this flag, removing an
     * Address from user.getAddresses() would just leave it "orphaned" —
     * still in the DB, still pointing at this user_id, just no longer
     * referenced in memory. That's rarely what you actually want for a
     * clearly-owned child like an address.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    public void addAddress(Address address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setUser(null);
    }
}