package org.ohdsi.circe.cohortdefinition.features;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureEnhancer {

    public static String addFeatureIntoSql(String sql, Feature feature) {

        try {
            Map<String, String> maskedVars = new HashMap<>();
            sql = maskVariables(sql, maskedVars);

            Select select = (Select) CCJSqlParserUtil.parse(sql);

            String targetAlias = feature.getAlias();

            select.accept(new TablesNamesFinder() {

                private boolean expressionApplied = false;

                private void addExpression(Node node, SelectExpressionItem selectItem) {

                    if (node instanceof SimpleNode) {
                        SimpleNode simpleNode = (SimpleNode) node;

                        if (simpleNode.jjtGetValue() instanceof PlainSelect) {
                            PlainSelect plainSelect = (PlainSelect) simpleNode.jjtGetValue();
                            plainSelect.getSelectItems().add(selectItem);
                            selectItem = new SelectExpressionItem(new Column(targetAlias));
                        }

                        if (simpleNode.jjtGetValue() instanceof SubSelect) {
                            SubSelect subSelect = (SubSelect) simpleNode.jjtGetValue();
                            ((Column) selectItem.getExpression()).setTable(new Table(subSelect.getAlias().getName()));
                        }
                    }

                    if (node.jjtGetParent() != null) {
                        addExpression(node.jjtGetParent(), selectItem);
                    }
                }

                private void processTable(Node node, String sourceAlias) {

                    try {
                        if (expressionApplied) {
                            throw new RuntimeException("Ambiguous source table");
                        }

                        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(
                                CCJSqlParserUtil.parseExpression(feature.getExpression(sourceAlias))
                        );
                        selectExpressionItem.setAlias(new Alias(targetAlias));

                        addExpression(node, selectExpressionItem);

                        expressionApplied = true;
                    } catch (JSQLParserException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public void visit(SubSelect subSelect) {

                    super.visit(subSelect);

                    String subSelectAlias = subSelect.getAlias().getName();
                    if (subSelectAlias.equalsIgnoreCase(feature.getTableName())) {
                        processTable(subSelect.getASTNode(), subSelectAlias);
                    }
                }

                @Override
                public void visit(Table table) {

                    if (table.getName().equalsIgnoreCase(feature.getTableName())) {
                        processTable(table.getASTNode(), table.getAlias() != null && table.getAlias().getName() != null ? table.getAlias().getName() : table.getName());
                    }
                }
            });

            sql = select.toString();
            for (Map.Entry<String, String> mask : maskedVars.entrySet()) {
                sql = sql.replaceAll(Pattern.quote(mask.getValue()), mask.getKey());
            }

            return sql;
        } catch (JSQLParserException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String maskVariables(String sql, Map<String, String> appliedReplacements) {

        String maskedSql = sql;
        Pattern p = Pattern.compile("@\\w+");
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String var = m.group();
            if (appliedReplacements.containsKey(var)) {
                continue;
            }
            String replacement;
            do {
                replacement = RandomStringUtils.randomAlphabetic(12);
            } while (maskedSql.contains(replacement));
            appliedReplacements.put(var, replacement);
            maskedSql = maskedSql.replaceAll(Pattern.quote(var), replacement);
        }
        return maskedSql;
    }
}
