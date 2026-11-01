package cz.softiq.service.automation.csssro.model;

import cz.softiq.common.base.model.IBaseModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlCreator implements IBaseModel {

  public static String createXmlDocument(EstateOfferBuilder estateOffer, String clientName) {
    try {
      // Create DocumentBuilder
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      // Create root element
      Document doc = docBuilder.newDocument();
      Element exportNode = doc.createElement("export");
      doc.appendChild(exportNode);
      Element offersNode = doc.createElement("nabidky");
      exportNode.appendChild(offersNode);
      Element rootElement = doc.createElement("nabidka");
      offersNode.appendChild(rootElement);

      // Add elements to the root
      rootElement.appendChild(
          createElement(doc, "nadpis_nemovitosti", estateOffer.getMainHeadline()));
      rootElement.appendChild(createElement(doc, "popis_nemovitosti", estateOffer.getMainText()));
      // node cena
      Element cena = doc.createElement("cena");
      cena.appendChild(createElement(doc, "castka", estateOffer.getCastka()));
      cena.appendChild(createElement(doc, "mena", estateOffer.getMena()));
      cena.appendChild(createElement(doc, "jednotka", estateOffer.getCenaCetnost()));
      rootElement.appendChild(cena);
      rootElement.appendChild(createElement(doc, "cost_of_living", estateOffer.getCostOfLiving()));
      // node adresa
      Element adresaUplnaElement = doc.createElement("adresa");
      adresaUplnaElement.appendChild(createElement(doc, "okres_nazev", estateOffer.getOkres()));
      adresaUplnaElement.appendChild(createElement(doc, "ulice_nazev", estateOffer.getUlice()));
      adresaUplnaElement.appendChild(createElement(doc, "obec_nazev", estateOffer.getObec()));
      adresaUplnaElement.appendChild(createElement(doc, "cisdom", estateOffer.getCisloDomu()));
      rootElement.appendChild(adresaUplnaElement);

      rootElement.appendChild(createElement(doc, "typ_nabidky", estateOffer.getTypSmlouvy()));
      rootElement.appendChild(
          createElement(doc, "engineering_networks", estateOffer.getInzenyrskeSite()));
      rootElement.appendChild(createElement(doc, "id_nabidka", estateOffer.getId()));
      rootElement.appendChild(createElement(doc, "updated", estateOffer.getDatumAktualizace()));
      rootElement.appendChild(createElement(doc, "ready_date", estateOffer.getKDispoziciOd()));
      rootElement.appendChild(createElement(doc, "building_type", estateOffer.getDruhObjektu()));
      rootElement.appendChild(
          createElement(doc, "object_location", estateOffer.getUmisteniObjektu()));
      rootElement.appendChild(createElement(doc, "furnished", estateOffer.getVybaveno()));
      rootElement.appendChild(createElement(doc, "building_area", estateOffer.getZastaveno()));
      rootElement.appendChild(createElement(doc, "floor_area", estateOffer.getCelkovaPlocha()));
      rootElement.appendChild(createElement(doc, "usable_area", estateOffer.getUzitnaPlocha()));
      rootElement.appendChild(createElement(doc, "loggie_area", estateOffer.getPocetLodzii()));
      rootElement.appendChild(createElement(doc, "loggie_count", estateOffer.getPocetLodzii()));
      rootElement.appendChild(createElement(doc, "electricity", estateOffer.getElektrina()));
      rootElement.appendChild(createElement(doc, "other", estateOffer.getOstatni()));
      rootElement.appendChild(createElement(doc, "stav", estateOffer.getStav()));
      rootElement.appendChild(
          createElement(doc, "enabled", String.valueOf(estateOffer.getEnabled())));
      rootElement.appendChild(
          createElement(doc, "lastExported", String.valueOf(estateOffer.getLastExported())));
      rootElement.appendChild(createElement(doc, "estate_building_type", estateOffer.getStavba()));
      rootElement.appendChild(
          createElement(doc, "telecommunication", estateOffer.getTelekomunikace()));
      rootElement.appendChild(createElement(doc, "heating", estateOffer.getTopeni()));
      rootElement.appendChild(createElement(doc, "cellar_count", estateOffer.getPocetSklepu()));
      rootElement.appendChild(createElement(doc, "cellar_area", estateOffer.getPlochaSklepa()));
      rootElement.appendChild(createElement(doc, "transport", estateOffer.getDoprava()));
      rootElement.appendChild(createElement(doc, "road_type", estateOffer.getKomunikace()));
      rootElement.appendChild(createElement(doc, "ready_date", estateOffer.getDatumNastehovani()));
      rootElement.appendChild(createElement(doc, "floors", estateOffer.getPocetPodlaziObjektu()));
      rootElement.appendChild(
          createElement(doc, "floor_number", estateOffer.getCisloPodlaziVDome()));
      rootElement.appendChild(createElement(doc, "typ_nemovitosti", estateOffer.getKategorie()));
      rootElement.appendChild(createElement(doc, "flat_kind", estateOffer.getDispoziceBytu()));
      rootElement.appendChild(createElement(doc, "ownership", estateOffer.getVlastnictvi()));
      rootElement.appendChild(createElement(doc, "building_condition", estateOffer.getStavBytu()));
      rootElement.appendChild(createElement(doc, "water", estateOffer.getVoda()));
      rootElement.appendChild(createElement(doc, "orientace", estateOffer.getOrientace()));
      rootElement.appendChild(createElement(doc, "gully", estateOffer.getOdpad()));
      rootElement.appendChild(createElement(doc, "gas", estateOffer.getPlyn()));
      rootElement.appendChild(createElement(doc, "poznamka", clientName));
      rootElement.appendChild(
          createElement(doc, "energy_efficiency_rating", estateOffer.getTrida()));
      rootElement.appendChild(
          createElement(doc, "energy_performance_certificate", estateOffer.getVyhlaska()));
      // node fotky
      Element fotky = doc.createElement("fotky");
      for (FotkaBuilder fotkaElement : estateOffer.getFotka()) {
        Element fotka = doc.createElement("fotka");
        fotka.appendChild(createElement(doc, "url", fotkaElement.getUrl()));
        fotka.appendChild(createElement(doc, "hlavni", String.valueOf(fotkaElement.getHlavni())));
        fotka.appendChild(createElement(doc, "poradi", String.valueOf(fotkaElement.getPoradi())));
        fotky.appendChild(fotka);
      }
      rootElement.appendChild(fotky);

      return xmlToString(doc);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static Element createElement(Document doc, String tagName, String textContent) {
    Element element = doc.createElement(tagName);
    element.appendChild(doc.createTextNode(textContent));
    return element;
  }

  private static String xmlToString(Document doc) {
    try {
      // Output the XML as a string
      javax.xml.transform.TransformerFactory transformerFactory =
          javax.xml.transform.TransformerFactory.newInstance();
      javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
      javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
      java.io.StringWriter writer = new java.io.StringWriter();
      javax.xml.transform.stream.StreamResult result =
          new javax.xml.transform.stream.StreamResult(writer);
      transformer.transform(source, result);
      return writer.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
