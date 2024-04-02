package com.cn.tvn.awscopy.model;

import com.cn.tvn.awscopy.model.status.FileStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class FileToCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    String sourceBucket;
    @Column(nullable = false)
    String sourceFile;
    @Column(nullable = false)
    String destBucket;
    @Column(nullable = false)
    String destFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    FileStatus status;

    Integer priority;

    @Column(nullable = true)
    Instant copyStartTimestamp;
    @Column(nullable = true)
    Instant copyFinishTimestamp;


    // https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private ObjectToCopy parentObject;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FileToCopy that = (FileToCopy) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
