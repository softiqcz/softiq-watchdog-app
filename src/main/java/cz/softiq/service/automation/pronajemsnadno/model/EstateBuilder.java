package cz.softiq.service.automation.pronajemsnadno.model;

import cz.softiq.common.base.model.IBaseModel;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EstateBuilder implements IBaseModel {
  private LocalDate created;
  private LocalDate updated;
  private String offerId;
}
