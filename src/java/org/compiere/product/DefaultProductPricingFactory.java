package org.compiere.product;

public class DefaultProductPricingFactory implements IProductPricingFactory {
	
	/**
	 * default constructor 
	 */
	public DefaultProductPricingFactory() {
	}

	@Override
	public AbstractProductPricing newProductPricingInstance() {
		return new MProductPricing();
	}

}

