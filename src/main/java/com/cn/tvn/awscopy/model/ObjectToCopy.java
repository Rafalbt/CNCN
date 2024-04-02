package com.cn.tvn.awscopy.model;

import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.utility.PrefixedObjectConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class ObjectToCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    String sourceBucket;

    @Column(nullable = false)
    @Convert(converter = PrefixedObjectConverter.class)
    PrefixedObject sourcePrefixedObject;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    ObjectStatus status;

    @Column(nullable = true)
    Instant listingStartTimestamp;
    @Column(nullable = true)
    Instant listingFinishTimestamp;

    @Column(nullable = true)
    Instant copyStartTimestamp;
    @Column(nullable = true)
    Instant copyFinishTimestamp;

    @OneToMany(
            mappedBy = "parentObject",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @OrderColumn(name = "priority")
    private List<FileToCopy> filesToCopy = new ArrayList<>();

    @JsonIgnore
    public Integer getFilesCount() {
        return Hibernate.size(filesToCopy);
    }

    @Column(updatable = false)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void setAutoFields() {
        this.createdAt = Instant.now();
    }

    // https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private ListToCopy parentList;

    @Transient
    @JsonInclude
    public Long getParentListId() {
        return parentList != null ? parentList.getId() : null;
    }

    public void addAllFilesToCopy(List<FileToCopy> orderedFilesToCopy) {
        this.filesToCopy.addAll(orderedFilesToCopy);
        this.filesToCopy.forEach(fileToCopy -> fileToCopy.setParentObject(this));
    }

    public void clearFilesToCopy() {
        this.filesToCopy.forEach(fileToCopy -> fileToCopy.setParentObject(null));
        this.filesToCopy.clear();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ObjectToCopy that = (ObjectToCopy) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public static List<ObjectToCopy> createObjectsToCopy(
                                                  String sourceBucket,
                                                  Set<PrefixedObject> prefixedObjects,
                                                  ObjectStatus status,
                                                  ListToCopy listToCopy) {
        return prefixedObjects.stream().map(prefixedObject ->
                ObjectToCopy.builder()
                        .sourceBucket(sourceBucket)
                        .sourcePrefixedObject(prefixedObject)
                        .status(status)
                        .parentList(listToCopy)
                        .build()).toList();
    }
}
