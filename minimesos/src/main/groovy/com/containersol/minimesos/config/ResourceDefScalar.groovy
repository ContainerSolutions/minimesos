package com.containersol.minimesos.config

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
/**
 * Check http://mesos.apache.org/documentation/latest/attributes-resources/ for possible types of values
 */
class ResourceDefScalar extends ResourceDef {

    private DecimalFormat format = null;
    private double value

    public ResourceDefScalar() {
    }

    public ResourceDefScalar(String role, double value) {
        super(role)
        this.value = value
    }

    private DecimalFormat getFormat() {
        if (format == null) {
            // see http://mesos.apache.org/documentation/latest/attributes-resources/
            // make format locale independent
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.' as char)
            format = new DecimalFormat("#.##", symbols)
        }
        return format
    }

    public void setValue(double value) {
        this.value = value
    }

    /**
     * Without this explicit setter groovy assigns unexpected values, if they are surrounded by ""
     * @param value to get double from
     */
    public void setValue(String value) {
        // correct format is intValue ( "." intValue )?
        if (value.contains(",")) {
            throw new NumberFormatException(value + " is not valid scalar value")
        }
        this.value = getFormat().parse(value).doubleValue()
    }

    public double getValue() {
        value
    }

    @Override
    String valueAsString() {
        return getFormat().format(value);
    }

}
