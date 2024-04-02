package com.cn.tvn.awscopy.model;

import com.cn.tvn.awscopy.model.status.ListStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class ListToCopy {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ListStatus status;

        @Column(updatable = false)
        @Setter(AccessLevel.NONE)
        @Builder.Default
        private Instant createdAt = Instant.now();

        @Setter(AccessLevel.NONE)
        @Builder.Default
        private Instant updatedAt = Instant.now();

        private String fileName;

        @PrePersist
        public void setAutoFields() {
                var now = Instant.now();
                this.createdAt = now;
                this.updatedAt = now;
        }

        @Override
        public final boolean equals(Object o) {
                if (this == o) return true;
                if (o == null) return false;
                Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
                Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
                if (thisEffectiveClass != oEffectiveClass) return false;
                ListToCopy that = (ListToCopy) o;
                return getId() != null && Objects.equals(getId(), that.getId());
        }

        @Override
        public final int hashCode() {
                return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
        }
}
