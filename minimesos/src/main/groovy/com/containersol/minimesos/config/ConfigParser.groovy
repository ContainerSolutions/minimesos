package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Parser for the minimesosFile. Turns minimesosFile with Groovy DSL specification into a {@link ClusterConfig} object.
 *
 * The minimesosFile DSL contains two components: blocks, and properties. A block starts and ends with curly braces: {}* and properties are nested inside the block. The main block is minimesos and it contains cluster-wide properties.
 * Other blocks contain properties that only affect the block itself like 'imageName' inside an agent block.
 */
@Slf4j
class ConfigParser {

    public static final String CONFIG_VARIABLE = "minimesos"

    private DecimalFormat format = null;

    private final Map<String, String> propsDictionary = [
            "agents": "agent",
            "cpus"  : "cpu",
            "mems"  : "mem",
            "disks" : "disk",
            "apps"  : "app"
    ]
    private final List<String> ignoredProperties = ["class", "format"]

    private final Map<String, String> comments = [
            "minimesos.marathon.apps": "Add 'app { marathonJson = \"<path or URL to JSON file>\" }' for every task you want to execute",
            "minimesos.marathon.cmd": "BEWARE: this option customize the marathon starting command, changing it can break the cluster"
    ]

    public ClusterConfig parse(String config) {
        Binding binding = new Binding()

        ClusterConfig minimesosDsl = new ClusterConfig()
        binding.setVariable(CONFIG_VARIABLE, minimesosDsl)

        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(config)
        script.run()

        return minimesosDsl
    }

    /**
     * Prints cluster configuration into a string
     *
     * @param config of the cluster to print
     * @return string representation of the cluster configuration
     */
    public String toString(ClusterConfig config) {
        String dslPath = CONFIG_VARIABLE
        StringBuilder buffer = new StringBuilder()
        appendPathDescription(buffer, "", dslPath)
        buffer.append(CONFIG_VARIABLE).append(" {\n")
        printProperties(buffer, "    ", config.properties, dslPath)
        buffer.append("}\n")

        buffer.toString()
    }

    private void printProperties(StringBuilder buffer, String intent, Map properties, String dslPath) {

        List<String> propNames = properties.keySet().sort()
        List<String> complexProps = new ArrayList<>()

        for (String propName : propNames) {
            if (!ignoredProperties.contains(propName)) {

                Object value = properties.get(propName)
                String strValue = formatSimpleValue(value)

                if (strValue != null) {
                    appendPathDescription(buffer, intent, dslPath + "." + propName)
                    String line = String.format("%s%s = %s\n", intent, propName, strValue)
                    buffer.append(line)
                } else {
                    complexProps.add(propName)
                }

            }
        }

        if (complexProps.size() > 0) {
            for (String propName : complexProps) {

                Object value = properties.get(propName)
                String propToPrint = propName
                if (propsDictionary.get(propName) != null) {
                    propToPrint = propsDictionary.get(propName)
                }

                if (Collection.class.isAssignableFrom(value.getClass())) {

                    Collection values = (Collection) value
                    printCollection(buffer, intent, propToPrint, values, dslPath + "." + propName)

                } else if (Map.class.isAssignableFrom(value.getClass())) {

                    Map values = (Map) value
                    printCollection(buffer, intent, propToPrint, values.values(), dslPath + "." + propName)

                } else {
                    buffer.append("\n").append(intent).append(propToPrint).append(" {\n")
                    printProperties(buffer, intent + "    ", value.properties, dslPath + "." + propName)
                    buffer.append(intent).append("}\n")
                }
            }
        }
    }

    private void appendPathDescription(StringBuilder buffer, String intent, String dslPath) {
        String comment = comments[dslPath]
        if (comment != null) {
            buffer.append(intent).append("// ").append(comment).append("\n")
        }
    }

    private String formatSimpleValue(Object value) {
        String strValue = null

        if (value == null) {
            strValue = "null"
        } else {

            Class clazz = value.getClass()
            if (String.class.isAssignableFrom(clazz)) {
                strValue = "\"" + value + "\""
            } else if (Integer.class.isAssignableFrom(clazz)) {
                strValue = value.toString()
            } else if (Boolean.class.isAssignableFrom(clazz)) {
                strValue = value.toString()
            } else if (Double.class.isAssignableFrom(clazz)) {
                strValue = getFormat().format(value);
            }

        }

        strValue
    }

    private void printCollection(StringBuilder buffer, String intent, String propName, Collection values, String dslPath) {
        buffer.append("\n")
        appendPathDescription(buffer, intent, dslPath)
        for (Object single : values) {
            String strSingle = formatSimpleValue(single)
            if (strSingle != null) {
                String line = String.format("%s%s = %s\n", intent, propName, strSingle)
                buffer.append(line)
            } else {
                buffer.append(intent).append(propName).append(" {\n")
                printProperties(buffer, intent + "    ", single.properties, dslPath)
            }
            buffer.append(intent).append("}\n")
        }
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
}
