package com.google.cloud.hadoop.io.bigquery.output;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.hadoop.io.bigquery.BigQueryConfiguration;
import com.google.cloud.hadoop.io.bigquery.BigQueryFileFormat;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class BigQueryOutputConfigurationTest {

  /** Sample projectId for the configuration. */
  private static final String TEST_PROJECT_ID = "domain:project";

  /** Sample datasetId for the configuration. */
  private static final String TEST_DATASET_ID = "dataset";

  /** Sample tableId for the configuration. */
  private static final String TEST_TABLE_ID = "table";

  /** Sample table reference. */
  private static final TableReference TEST_TABLE_REF =
      new TableReference()
          .setProjectId(TEST_PROJECT_ID)
          .setDatasetId(TEST_DATASET_ID)
          .setTableId(TEST_TABLE_ID);

  /** Sample output file format for the configuration. */
  private static final BigQueryFileFormat TEST_FILE_FORMAT =
      BigQueryFileFormat.NEWLINE_DELIMITED_JSON;

  /** Sample output format class for the configuration. */
  @SuppressWarnings("rawtypes")
  private static final Class<? extends FileOutputFormat> TEST_OUTPUT_CLASS = TextOutputFormat.class;

  /** Sample table schema used for output. */
  private static final TableSchema TEST_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              new ArrayList<TableFieldSchema>() {
                {
                  add(new TableFieldSchema().setName("A").setType("STRING"));
                  add(new TableFieldSchema().setName("B").setType("INTEGER"));
                }
              });

  /** Sample expected serialized version of TEST_TABLE_SCHEMA. */
  private static final String TEST_TABLE_SCHEMA_STRING =
      "{\"fields\":[{\"name\":\"A\",\"type\":\"STRING\"},{\"name\":\"B\",\"type\":\"INTEGER\"}]}";

  /** Sample malformed serialized version of TEST_TABLE_SCHEMA. */
  private static final String TEST_BAD_TABLE_SCHEMA_STRING =
      "{\"fields\":[{name:\"A\",type:\"STRING\"},{name:\"B\",type:\"INTEGER\"}]}";

  /** The Job Configuration for testing. */
  private static JobConf conf;

  @Mock private JobID mockJobID;

  /** Verify exceptions are being thrown. */
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  /** Set up before all classes. */
  @Before
  public void setUp() {
    // Generate Mocks.
    MockitoAnnotations.initMocks(this);

    // Create the configuration.
    conf = new JobConf();

    new BigQueryOutputConfiguration();
  }

  /** Test the configure function correctly sets the configuration keys. */
  @Test
  public void testConfigure() {
    BigQueryOutputConfiguration.configure(
        conf,
        TEST_PROJECT_ID,
        TEST_DATASET_ID,
        TEST_TABLE_ID,
        TEST_FILE_FORMAT,
        TEST_OUTPUT_CLASS,
        TEST_TABLE_SCHEMA);

    assertThat(conf.get(BigQueryOutputConfiguration.PROJECT_ID), is(TEST_PROJECT_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.DATASET_ID), is(TEST_DATASET_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.TABLE_ID), is(TEST_TABLE_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.FILE_FORMAT), is(TEST_FILE_FORMAT.name()));
    assertThat(
        conf.get(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS), is(TEST_OUTPUT_CLASS.getName()));
    assertThat(conf.get(BigQueryOutputConfiguration.TABLE_SCHEMA), is(TEST_TABLE_SCHEMA_STRING));
  }

  /** Test the configure function correctly sets the configuration keys. */
  @Test
  public void testConfigureHelper() {
    BigQueryOutputConfiguration.configure(
        conf,
        TEST_PROJECT_ID + ":" + TEST_DATASET_ID + "." + TEST_TABLE_ID,
        TEST_FILE_FORMAT,
        TEST_OUTPUT_CLASS,
        TEST_TABLE_SCHEMA);

    assertThat(conf.get(BigQueryOutputConfiguration.PROJECT_ID), is(TEST_PROJECT_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.DATASET_ID), is(TEST_DATASET_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.TABLE_ID), is(TEST_TABLE_ID));
    assertThat(conf.get(BigQueryOutputConfiguration.FILE_FORMAT), is(TEST_FILE_FORMAT.name()));
    assertThat(
        conf.get(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS), is(TEST_OUTPUT_CLASS.getName()));
    assertThat(conf.get(BigQueryOutputConfiguration.TABLE_SCHEMA), is(TEST_TABLE_SCHEMA_STRING));
  }

  /** Test an exception is thrown if a required parameter is missing for configure. */
  @Test
  public void testConfigureMissing() {
    expectedException.expect(IllegalArgumentException.class);

    // Missing the PROJECT_ID.
    BigQueryOutputConfiguration.configure(
        conf,
        null,
        TEST_DATASET_ID,
        TEST_TABLE_ID,
        TEST_FILE_FORMAT,
        TEST_OUTPUT_CLASS,
        TEST_TABLE_SCHEMA);
  }

  /** Test the validateConfiguration function doesn't error on expected input. */
  @Test
  public void testValidateConfiguration() throws IOException {
    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);
    conf.set(BigQueryOutputConfiguration.FILE_FORMAT, TEST_FILE_FORMAT.name());
    conf.set(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS, TEST_OUTPUT_CLASS.getName());
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_TABLE_SCHEMA_STRING);

    BigQueryOutputConfiguration.validateConfiguration(conf);
  }

  /** Test the validateConfiguration throws an exception on a missing required key. */
  @Test
  public void testValidateConfigurationMissingKey() throws IOException {
    expectedException.expect(IOException.class);

    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);
    conf.set(BigQueryOutputConfiguration.FILE_FORMAT, TEST_FILE_FORMAT.name());
    conf.set(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS, TEST_OUTPUT_CLASS.getName());
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_TABLE_SCHEMA_STRING);
    // Missing DATASET_ID

    BigQueryOutputConfiguration.validateConfiguration(conf);
  }

  /** Test the validateConfiguration throws an exception for a malformed schema. */
  @Test
  public void testValidateConfigurationBadSchema() throws IOException {
    expectedException.expect(IOException.class);

    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);
    conf.set(BigQueryOutputConfiguration.FILE_FORMAT, TEST_FILE_FORMAT.name());
    conf.set(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS, TEST_OUTPUT_CLASS.getName());
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_BAD_TABLE_SCHEMA_STRING);

    BigQueryOutputConfiguration.validateConfiguration(conf);
  }

  /** Test the validateConfiguration throws an exception for a malformed file format. */
  @Test
  public void testValidateConfigurationBadFileformat() throws IOException {
    expectedException.expect(IllegalArgumentException.class);

    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);
    conf.set(BigQueryOutputConfiguration.FILE_FORMAT, TEST_FILE_FORMAT.name().toLowerCase());
    conf.set(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS, TEST_OUTPUT_CLASS.getName());
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_TABLE_SCHEMA_STRING);

    BigQueryOutputConfiguration.validateConfiguration(conf);
  }

  /** Test the validateConfiguration throws an exception for an incorrect output format class. */
  @Test
  public void testValidateConfigurationWrongOutputClass() throws IOException {
    expectedException.expect(IOException.class);

    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);
    conf.set(BigQueryOutputConfiguration.FILE_FORMAT, TEST_FILE_FORMAT.name());
    conf.set(BigQueryOutputConfiguration.OUTPUT_FORMAT_CLASS, OutputFormat.class.getName());
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_TABLE_SCHEMA_STRING);

    BigQueryOutputConfiguration.validateConfiguration(conf);
  }

  /** Test the getTable returns the correct data. */
  @Test
  public void testGetTable() throws IOException {
    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_TABLE_ID);

    assertThat(BigQueryOutputConfiguration.getTableReference(conf), is(TEST_TABLE_REF));
  }

  /** Test the getTable throws an exception when a key is missing. */
  @Test
  public void testGetTableMissingKey() throws IOException {
    expectedException.expect(IOException.class);

    conf.set(BigQueryOutputConfiguration.PROJECT_ID, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    // Missing TABLE_ID

    BigQueryOutputConfiguration.getTableReference(conf);
  }

  /** Test the getTable does not throw an exception when a backup project id is found. */
  @Test
  public void testGetTableBackupProjectId() throws IOException {
    conf.set(BigQueryConfiguration.PROJECT_ID_KEY, TEST_PROJECT_ID);
    conf.set(BigQueryOutputConfiguration.DATASET_ID, TEST_DATASET_ID);
    conf.set(BigQueryOutputConfiguration.TABLE_ID, TEST_PROJECT_ID);

    BigQueryOutputConfiguration.getTableReference(conf);
  }

  /** Test the getTableSchema returns the correct data. */
  @Test
  public void testGetTableSchema() throws IOException {
    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_TABLE_SCHEMA_STRING);

    assertThat(BigQueryOutputConfiguration.getTableSchema(conf), is(TEST_TABLE_SCHEMA));
  }

  /** Test the getTableSchema throws an exception when the schema is malformed. */
  @Test
  public void testGetTableSchemaBadSchema() throws IOException {
    expectedException.expect(IOException.class);

    conf.set(BigQueryOutputConfiguration.TABLE_SCHEMA, TEST_BAD_TABLE_SCHEMA_STRING);

    BigQueryOutputConfiguration.getTableSchema(conf);
  }
}