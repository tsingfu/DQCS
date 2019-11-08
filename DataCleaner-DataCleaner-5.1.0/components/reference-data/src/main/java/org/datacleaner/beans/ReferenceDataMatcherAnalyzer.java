/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.beans;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.datacleaner.api.Alias;
import org.datacleaner.api.Analyzer;
import org.datacleaner.api.Close;
import org.datacleaner.api.Configured;
import org.datacleaner.api.Description;
import org.datacleaner.api.Distributed;
import org.datacleaner.api.Initialize;
import org.datacleaner.api.InputColumn;
import org.datacleaner.api.InputRow;
import org.datacleaner.api.OutputColumns;
import org.datacleaner.api.Provided;
import org.datacleaner.api.Validate;
import org.datacleaner.beans.transform.DictionaryMatcherTransformer;
import org.datacleaner.beans.transform.StringPatternMatcherTransformer;
import org.datacleaner.components.convert.ConvertToStringTransformer;
import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.data.MockInputColumn;
import org.datacleaner.data.MockInputRow;
import org.datacleaner.reference.Dictionary;
import org.datacleaner.reference.StringPattern;
import org.datacleaner.reference.SynonymCatalog;
import org.datacleaner.reference.SynonymCatalogConnection;

@Named("Reference data matcher")
@Alias("Matching analyzer")
@Description("Check your data values against multiple forms of reference data in one simple analyzer step.\n"
        + "This analyzer provides a handy shortcut for doing matching with dictionaries, synonym lookups or string patterns matching, retrieving matching matrices for all matches.")
@Distributed(reducer = BooleanAnalyzerReducer.class)
public class ReferenceDataMatcherAnalyzer implements Analyzer<BooleanAnalyzerResult> {

    @Configured(order = 1)
    InputColumn<?>[] columns;

    @Configured(order = 2, required = false)
    Dictionary[] dictionaries;

    @Configured(order = 3, required = false)
    SynonymCatalog[] synonymCatalogs;

    @Configured(order = 4, required = false)
    StringPattern[] stringPatterns;

    @Provided
    DataCleanerConfiguration configuration;

    private BooleanAnalyzer _booleanAnalyzer;
    private DictionaryMatcherTransformer[] _dictionaryMatchers;
    private StringPatternMatcherTransformer[] _stringPatternMatchers;
    private SynonymCatalogConnection[] _synonymCatalogConnections;
    private List<InputColumn<Boolean>> _matchColumns;

    public ReferenceDataMatcherAnalyzer(InputColumn<?>[] columns, Dictionary[] dictionaries,
            SynonymCatalog[] synonymCatalogs, StringPattern[] stringPatterns, DataCleanerConfiguration configuration) {
        this();
        this.columns = columns;
        this.dictionaries = dictionaries;
        this.stringPatterns = stringPatterns;
        this.synonymCatalogs = synonymCatalogs;
        this.configuration = configuration;
    }

    public ReferenceDataMatcherAnalyzer() {
    }

    @Validate
    public void validate() {
        if (!isDictionaryMatchingEnabled() && !isSynonymCatalogLookupEnabled() && !isStringPatternMatchingEnabled()) {
            throw new IllegalStateException("No dictionaries, synonym catalogs or string patterns selected");
        }
    }

    @Initialize
    public void init() {
        _dictionaryMatchers = new DictionaryMatcherTransformer[columns.length];
        _stringPatternMatchers = new StringPatternMatcherTransformer[columns.length];

        _matchColumns = new ArrayList<InputColumn<Boolean>>();

        OutputColumns outputColumns;
        for (int i = 0; i < columns.length; i++) {
            if (isDictionaryMatchingEnabled()) {
                // create matcher for dictionaries
                DictionaryMatcherTransformer dictionaryMatcher = new DictionaryMatcherTransformer(columns[i],
                        dictionaries, configuration);
                dictionaryMatcher.init();
                outputColumns = dictionaryMatcher.getOutputColumns();
                addMatchColumns(outputColumns);
                _dictionaryMatchers[i] = dictionaryMatcher;
            }

            if (isSynonymCatalogLookupEnabled()) {
                outputColumns = new OutputColumns(synonymCatalogs.length, Boolean.class);
                _synonymCatalogConnections = new SynonymCatalogConnection[synonymCatalogs.length];
                for (int j = 0; j < synonymCatalogs.length; j++) {
                    SynonymCatalog synonymCatalog = synonymCatalogs[j];
                    _synonymCatalogConnections[j] = synonymCatalog.openConnection(configuration);
                    outputColumns.setColumnName(j, columns[i].getName() + " in " + synonymCatalog.getName());
                }
                addMatchColumns(outputColumns);
            }

            if (isStringPatternMatchingEnabled()) {
                // create matcher for string patterns
                StringPatternMatcherTransformer stringPatternMatcher = new StringPatternMatcherTransformer(columns[i],
                        stringPatterns, configuration);
                stringPatternMatcher.init();
                outputColumns = stringPatternMatcher.getOutputColumns();
                addMatchColumns(outputColumns);
                _stringPatternMatchers[i] = stringPatternMatcher;
            }
        }

        @SuppressWarnings("unchecked")
        InputColumn<Boolean>[] columnArray = _matchColumns.toArray(new InputColumn[_matchColumns.size()]);
        _booleanAnalyzer = new BooleanAnalyzer(columnArray);
        _booleanAnalyzer.init();
    }

    @Close
    public void close() {
        if (isDictionaryMatchingEnabled() && _dictionaryMatchers != null) {
            for (DictionaryMatcherTransformer matcher : _dictionaryMatchers) {
                matcher.close();
            }
            _dictionaryMatchers = null;
        }
        if (isStringPatternMatchingEnabled() && _stringPatternMatchers != null) {
            for (StringPatternMatcherTransformer matcher : _stringPatternMatchers) {
                matcher.close();
            }
            _stringPatternMatchers = null;
        }
        if (isSynonymCatalogLookupEnabled() && _synonymCatalogConnections != null) {
            for (SynonymCatalogConnection connection : _synonymCatalogConnections) {
                connection.close();
            }
            _synonymCatalogConnections = null;
        }
    }

    private boolean isStringPatternMatchingEnabled() {
        return stringPatterns != null && stringPatterns.length > 0;
    }

    private boolean isSynonymCatalogLookupEnabled() {
        return synonymCatalogs != null && synonymCatalogs.length > 0;
    }

    private boolean isDictionaryMatchingEnabled() {
        return dictionaries != null && dictionaries.length > 0;
    }

    private void addMatchColumns(OutputColumns outputColumns) {
        int count = outputColumns.getColumnCount();
        for (int i = 0; i < count; i++) {
            String columnName = outputColumns.getColumnName(i);
            InputColumn<Boolean> col = new MockInputColumn<Boolean>(columnName, Boolean.class);
            _matchColumns.add(col);
        }
    }

    @Override
    public void run(InputRow row, int distinctCount) {
        MockInputRow mockInputRow = new MockInputRow();

        int matchColumnIndex = 0;
        for (int i = 0; i < columns.length; i++) {
            final Object value = row.getValue(columns[i]);
            final String stringValue = ConvertToStringTransformer.transformValue(value);
            mockInputRow.put(columns[i], value);

            if (isDictionaryMatchingEnabled()) {
                Object[] matches = _dictionaryMatchers[i].transform(row);
                for (Object match : matches) {
                    assert match instanceof Boolean;

                    InputColumn<Boolean> matchColumn = _matchColumns.get(matchColumnIndex);
                    matchColumnIndex++;
                    mockInputRow.put(matchColumn, match);
                }
            }

            if (isSynonymCatalogLookupEnabled()) {
                for (SynonymCatalogConnection synonymCatalogConnection : _synonymCatalogConnections) {
                    final InputColumn<Boolean> matchColumn = _matchColumns.get(matchColumnIndex);
                    matchColumnIndex++;
                    final String masterTerm = synonymCatalogConnection.getMasterTerm(stringValue);
                    if (masterTerm == null) {
                        // no match
                        mockInputRow.put(matchColumn, Boolean.FALSE);
                    } else {
                        mockInputRow.put(matchColumn, Boolean.TRUE);
                    }
                }
            }

            if (isStringPatternMatchingEnabled()) {
                Object[] matches = _stringPatternMatchers[i].transform(row);
                for (Object match : matches) {
                    assert match instanceof Boolean;
                    InputColumn<Boolean> matchColumn = _matchColumns.get(matchColumnIndex);
                    matchColumnIndex++;
                    mockInputRow.put(matchColumn, match);
                }
            }
        }

        _booleanAnalyzer.run(mockInputRow, distinctCount);
    }

    @Override
    public BooleanAnalyzerResult getResult() {
        return _booleanAnalyzer.getResult();
    }
}
