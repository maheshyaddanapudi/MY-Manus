package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Visualization Tool - Creates charts and graphs
 *
 * Manus AI Equivalent: Agents can create visualizations using matplotlib/plotly
 *
 * Note: This is a thin wrapper - actual visualization happens via Python code
 * The agent writes Python code that uses matplotlib/plotly/seaborn
 * This tool just provides a convenient interface and documentation
 */
@Slf4j
@Component
public class DataVisualizationTool implements Tool {

    @Override
    public String getName() {
        return "visualize_data";
    }

    @Override
    public String getDescription() {
        return "Create data visualizations. Supported types: line, bar, scatter, histogram, heatmap. " +
               "Returns guidance on which Python visualization libraries to use. " +
               "Agent should write Python code using matplotlib, seaborn, or plotly.";
    }

    @Override
    public String getPythonSignature() {
        return "visualize_data(chart_type: str, data_description: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String chartType = (String) parameters.get("chart_type");
        String dataDescription = (String) parameters.get("data_description");

        log.info("📊 Visualization request: {}", chartType);

        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("chartType", chartType);
        result.put("guidance", getVisualizationGuidance(chartType));

        return result;
    }

    private String getVisualizationGuidance(String chartType) {
        return switch (chartType != null ? chartType.toLowerCase() : "") {
            case "line" -> """
                Use matplotlib.pyplot.plot() or seaborn.lineplot()
                Example:
                import matplotlib.pyplot as plt
                plt.plot(x, y)
                plt.savefig('/workspace/chart.png')
                """;

            case "bar" -> """
                Use matplotlib.pyplot.bar() or seaborn.barplot()
                Example:
                import matplotlib.pyplot as plt
                plt.bar(categories, values)
                plt.savefig('/workspace/chart.png')
                """;

            case "scatter" -> """
                Use matplotlib.pyplot.scatter() or seaborn.scatterplot()
                Example:
                import matplotlib.pyplot as plt
                plt.scatter(x, y)
                plt.savefig('/workspace/chart.png')
                """;

            case "histogram" -> """
                Use matplotlib.pyplot.hist() or seaborn.histplot()
                Example:
                import matplotlib.pyplot as plt
                plt.hist(data, bins=20)
                plt.savefig('/workspace/chart.png')
                """;

            case "heatmap" -> """
                Use seaborn.heatmap()
                Example:
                import seaborn as sns
                sns.heatmap(data, annot=True)
                plt.savefig('/workspace/chart.png')
                """;

            default -> """
                Supported chart types: line, bar, scatter, histogram, heatmap
                Use matplotlib, seaborn, or plotly to create visualizations
                Save charts to /workspace/ directory
                """;
        };
    }

    @Override
    public boolean requiresNetwork() {
        return false;
    }
}
