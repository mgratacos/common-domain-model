package org.isda.cdm.functions;

import com.google.inject.Inject;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import org.isda.cdm.CollateralRounding;
import org.isda.cdm.MarginApproachEnum;
import org.isda.cdm.Money;
import org.isda.cdm.PostedCreditSupportItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryAmountTest extends AbstractFunctionTest {

	@Inject private DeliveryAmount deliveryAmountCalc;

	private static final String BASE_CURRENCY_USD = "EUR";
	private static final String CURRENCY_SCHEME = "http://www.fpml.org/coding-scheme/external/iso4217";
	private static final double EPSILON = 1e-10;

	@Test
	void shouldCalculateDeliveryAmount() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(10, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithValuationPercentageAndFxHaircutPercentage() {
		double deliveryAmount = deliveryAmount(
				5,
				90,
				8,
				0,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.01,
				0);
		assertEquals(10.9, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithDisputedPostedCreditSupportAmount() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				2,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(12, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithPriorDeliveryAmountAdjustment() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				1,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(9, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithPriorReturnAmountAdjustment() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				1,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(11, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithDisputedTransferredPostedCreditSupportAmount() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				2,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(12, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithThreshold() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				0,
				15,
				10,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(0, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithMarginApproachGreaterOf() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.GREATER_OF,
				18d,
				0,
				0.5,
				0);
		assertEquals(13, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithMinimumTransferAmount() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				16,
				0.5,
				0);
		assertEquals(0, deliveryAmount, EPSILON);
	}

	@Test
	void shouldCalculateDeliveryAmountWithDisputedDeliveryAmount() {
		double deliveryAmount = deliveryAmount(
				5,
				100,
				0,
				0,
				0,
				0,
				0,
				15,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				3.5);
		assertEquals(6.5, deliveryAmount, EPSILON);
	}

	private double deliveryAmount(
			double postedCreditSupportAmount,
			double valuationPercentage,
			double fxHaircutPercentage,
			double disputedPostedCreditSupportAmount,
			double priorDeliveryAmountAdjustment,
			double priorReturnAmountAdjustment,
			double disputedTransferredPostedCreditSupportAmount,
			double marginAmount,
			double threshold,
			MarginApproachEnum marginApproach,
			Double marginAmountIA,
			double minimumTransferAmount,
			double roundToNearest,
			double disputedDeliveryAmount
	) {
		List<PostedCreditSupportItem> postedCreditSupportItems = Collections.singletonList(PostedCreditSupportItem.builder()
				.setCashOrSecurityValue(getMoney(postedCreditSupportAmount))
				.setValuationPercentage(BigDecimal.valueOf(valuationPercentage))
				.setFxHaircutPercentage(BigDecimal.valueOf(fxHaircutPercentage))
				.setDisputedCashOrSecurityValue(getMoney(disputedPostedCreditSupportAmount))
				.build());

		CollateralRounding rounding = CollateralRounding.builder()
				.setDeliveryAmount(BigDecimal.valueOf(roundToNearest))
				.setReturnAmount(BigDecimal.valueOf(roundToNearest))
				.build();

		Money deliveryAmountMoney = deliveryAmountCalc.evaluate(postedCreditSupportItems,
				getMoney(priorDeliveryAmountAdjustment),
				getMoney(priorReturnAmountAdjustment),
				getMoney(disputedTransferredPostedCreditSupportAmount),
				getMoney(marginAmount),
				getMoney(threshold),
				marginApproach,
				Optional.ofNullable(marginAmountIA).map(this::getMoney).orElse(null),
				getMoney(minimumTransferAmount),
				rounding,
				getMoney(disputedDeliveryAmount),
				BASE_CURRENCY_USD);

		return deliveryAmountMoney.getAmount().doubleValue();
	}

	private Money getMoney(double amount) {
		return Money.builder()
				.setAmount(BigDecimal.valueOf(amount))
				.setCurrency(FieldWithMetaString.builder()
						.setValue(BASE_CURRENCY_USD)
						.setMetaBuilder(MetaFields.builder()
								.setScheme(CURRENCY_SCHEME))
						.build())
				.build();
	}
}
