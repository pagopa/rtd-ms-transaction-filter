package it.gov.pagopa.rtd.transaction_filter.service.store;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class AggregationData {

  public static final String DIRTY_CURRENCY = "###na###";
  public static final byte DIRTY_POS_TYPE = 127;
  public static final byte INIT_POS_TYPE = 126;
  public static final String INIT_VAT = "###init###";
  public static final String DIRTY_VAT = "###na###";

  private int numTrx;
  // Integer should be fine until we aggregate on daily basis.
  // Remember to re-evaluate the data type in case the aggregation period would be increased.
  private int totalAmount;
  private String vat = INIT_VAT;
  private byte posType = INIT_POS_TYPE;
  private Currency currency;

  public void incNumTrx() {
    this.numTrx += 1;
  }

  public void incTotalAmount(int amount) {
    this.totalAmount += amount;
  }

  public void updateCurrencyOrMarkAsDirty(String currency) {
    if (this.getCurrency() == null) {
      this.setCurrency(CurrencyFlyweight.createCurrency(currency));
    } else {
      if (!this.getCurrency().getIsoCode().equals(DIRTY_CURRENCY) && !this.getCurrency().getIsoCode().equals(currency)) {
        this.setCurrency(CurrencyFlyweight.createCurrency(DIRTY_CURRENCY));
      }
    }
  }

  public void updateVatOrMarkAsDirty(String vat) {
    if (this.getVat() != null) {
      if (this.getVat().equals(INIT_VAT)) {
        this.setVat(vat);
      } else {
        if (!this.getVat().equals(vat)) {
          this.setVat(DIRTY_VAT);
        }
      }
    } else {
      if (vat != null) {
        this.setVat(DIRTY_VAT);
      }
    }
  }

  public void updatePosTypeOrMarkAsDirty(String posType) {
    if (this.getPosType() == INIT_POS_TYPE) {
      if (posType.equals("00")) {
        this.setPosType((byte) 0);
      } else {
        this.setPosType((byte) 1);
      }
    } else {
      if (posType.equals("00") && this.getPosType() != 0) {
        this.setPosType(DIRTY_POS_TYPE);
      }
      if (posType.equals("01") && this.getPosType() != 1) {
        this.setPosType(DIRTY_POS_TYPE);
      }
    }
  }

}

