package org.aueb.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "checkpoints")
public class Checkpoint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "checkpoint_id")
    private int checkpointId;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    public Checkpoint() {}

    public Checkpoint(String name) {
        setName(name);
    }

    public int getCheckpointId() {
        return checkpointId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Checkpoint)) return false;
        Checkpoint that = (Checkpoint) o;

        if (checkpointId != 0 && that.checkpointId != 0)
            return checkpointId == that.checkpointId;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkpointId);
    }

    @Override
    public String toString() {
        return "Checkpoint{" +
                "checkpointId=" + checkpointId +
                ", name='" + name + '\'' +
                '}';
    }
}
