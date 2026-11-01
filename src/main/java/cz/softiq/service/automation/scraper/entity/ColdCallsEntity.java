package cz.softiq.service.automation.scraper.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ColdCallsEntity implements IBaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column private String fullName;
  @Column private String email;
  @Column private String source;

  @Column(columnDefinition = "boolean default false")
  private Boolean isContacted = false;

  public ColdCallsEntity(String fullName, String email, String source) {
    this.fullName = fullName;
    this.email = email;
    this.source = source;
  }
}
