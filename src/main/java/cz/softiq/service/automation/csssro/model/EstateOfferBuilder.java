package cz.softiq.service.automation.csssro.model;

import cz.softiq.common.base.model.IBaseModel;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EstateOfferBuilder implements IBaseModel {
  // dulezite info
  private String mainHeadline;
  private Instant lastExported;
  // cena
  private String castka;
  private String mena;
  private String cenaCetnost;
  private String costOfLiving;
  private String mainText;
  // adresa
  private String okres;
  private String ulice;
  private String obec;
  private String cisloDomu;
  private String typSmlouvy;
  private String inzenyrskeSite;
  private String id;
  private String datumAktualizace;
  private String kDispoziciOd;

  private String druhObjektu;
  private String zastaveno;
  private String umisteniObjektu;
  private String vybaveno;
  private String celkovaPlocha;
  private String uzitnaPlocha;
  private String plochaLodzie;
  private String pocetLodzii;
  private String elektrina;
  private String ostatni;
  private String stavba;
  private String plochaSklepa;
  private String pocetSklepu;
  private String telekomunikace;
  private String topeni;
  private String doprava;
  private String komunikace;
  private String datumNastehovani;
  private String pocetPodlaziObjektu;
  private String cisloPodlaziVDome;

  private String kategorie;
  private String dispoziceBytu;
  private String vlastnictvi;
  private String stavBytu;
  private String voda;
  private String orientace;
  private String odpad;
  private String plyn;
  // energeticka narocnost
  private String trida;
  private String vyhlaska;
  // dostupnost
  private String stav;
  private Boolean enabled;
  // fotky
  private List<FotkaBuilder> fotka;
}
