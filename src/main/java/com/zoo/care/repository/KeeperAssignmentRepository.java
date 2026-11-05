package com.zoo.care.repository;

import com.zoo.care.entity.KeeperAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KeeperAssignmentRepository extends JpaRepository<KeeperAssignment, Long> {


    @Query("""
          select case when count(a) > 0 then true else false end
          from KeeperAssignment a
          where a.enclosure.id = :enclosureId
            and a.keeper.id = :keeperId
            and a.startsAt <= :moment
            and (a.endsAt is null or a.endsAt > :moment)
          """)
    boolean existsActiveAssignment(
            @Param("enclosureId") Long enclosureId,
            @Param("keeperId") Long keeperId,
            @Param("moment") Instant moment
    );

    @Query("""
          select a from KeeperAssignment a
          where a.enclosure.id = :enclosureId
            and a.keeper.id = :keeperId
            and a.startsAt <= :moment
            and (a.endsAt is null or a.endsAt > :moment)
          """)
    Optional<KeeperAssignment> findActive(@Param("enclosureId") Long enclosureId,
                                          @Param("keeperId") Long keeperId,
                                          @Param("moment") Instant moment);

    @Query("""
          select a from KeeperAssignment a
          join fetch a.keeper
          where a.enclosure.id = :enclosureId
            and a.startsAt <= :moment
            and (a.endsAt is null or a.endsAt > :moment)
          order by a.keeper.fullName
          """)
    List<KeeperAssignment> findActiveByEnclosure(@Param("enclosureId") Long enclosureId,
                                                 @Param("moment") Instant moment);

    void deleteByEnclosureId(Long enclosureId);
}
