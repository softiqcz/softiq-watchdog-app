package cz.softiq.service.automation.scraper.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.automation.scraper.entity.ColdCallsEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScraperRepository
    extends CrudRepository<ColdCallsEntity, Integer>, IBaseRepository {
  ColdCallsEntity findByEmail(String email);

  @Query(
      value = "SELECT * FROM cold_calls_entity WHERE is_contacted = 0 ORDER BY RAND() LIMIT :limit",
      nativeQuery = true)
  List<ColdCallsEntity> findAllRandom(@Param("limit") int limit);
}
