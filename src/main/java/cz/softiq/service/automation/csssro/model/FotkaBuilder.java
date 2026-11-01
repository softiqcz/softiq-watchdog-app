package cz.softiq.service.automation.csssro.model;

import cz.softiq.common.base.model.IBaseModel;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FotkaBuilder implements IBaseModel {
  private String url;
  @Builder.Default private Boolean hlavni = false;
  @Builder.Default private int poradi = 1;
}
