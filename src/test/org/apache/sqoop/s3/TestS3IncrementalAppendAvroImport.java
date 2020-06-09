/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sqoop.s3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.sqoop.testutil.ArgumentArrayBuilder;
import org.apache.sqoop.testutil.AvroTestUtils;
import org.apache.sqoop.testutil.DefaultS3CredentialGenerator;
import org.apache.sqoop.testutil.ImportJobTestCase;
import org.apache.sqoop.testutil.S3CredentialGenerator;
import org.apache.sqoop.testutil.S3TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.apache.sqoop.util.AppendUtils.MAPREDUCE_OUTPUT_BASENAME_PROPERTY;

public class TestS3IncrementalAppendAvroImport extends ImportJobTestCase {

    public static final Log LOG = LogFactory.getLog(
            TestS3IncrementalAppendAvroImport.class.getName());

    private static S3CredentialGenerator s3CredentialGenerator;

    private FileSystem s3Client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setupS3Credentials() throws IOException {
        String generatorCommand = S3TestUtils.getGeneratorCommand();
        if (generatorCommand != null) {
            s3CredentialGenerator = new DefaultS3CredentialGenerator(generatorCommand);
        }
    }

    @Before
    public void setup() throws IOException {
        S3TestUtils.runTestCaseOnlyIfS3CredentialsAreSet(s3CredentialGenerator);
        super.setUp();
        S3TestUtils.createTestTableFromInputData(this);
        s3Client = S3TestUtils.setupS3ImportTestCase(s3CredentialGenerator);
    }

    @After
    public void cleanUpOutputDirectories() {
        S3TestUtils.tearDownS3IncrementalImportTestCase(s3Client);
        super.tearDown();
    }

    @Test
    public void testS3IncrementalAppendAsAvroDataFileWhenNoNewRowIsImported() throws IOException {
        String[] args = getArgsWithAsAvroDataFileOption(false);
        runImport(args);

        args = getIncrementalAppendArgsWithAsAvroDataFileOption(false);
        runImport(args);

        S3TestUtils.failIfOutputFilePathContainingPatternExists(s3Client, MAP_OUTPUT_FILE_00001);
    }

    @Test
    public void testS3IncrementalAppendAsAvroDataFile() throws IOException {
        String[] args = getArgsWithAsAvroDataFileOption(false);
        runImport(args);

        S3TestUtils.insertInputDataIntoTable(this, S3TestUtils.getExtraInputData());

        args = getIncrementalAppendArgsWithAsAvroDataFileOption(false);
        runImport(args);

        AvroTestUtils.verify(S3TestUtils.getExpectedExtraAvroOutput(), s3Client.getConf(), S3TestUtils.getTargetDirPath(), MAP_OUTPUT_FILE_00001);
    }

    @Test
    public void testS3IncrementalAppendAsAvroDataFileWithMapreduceOutputBasenameProperty() throws IOException {
        String[] args = getArgsWithAsAvroDataFileOption(true);
        runImport(args);

        S3TestUtils.insertInputDataIntoTable(this, S3TestUtils.getExtraInputData());

        args = getIncrementalAppendArgsWithAsAvroDataFileOption(true);
        runImport(args);

        AvroTestUtils.verify(S3TestUtils.getExpectedExtraAvroOutput(), s3Client.getConf(), S3TestUtils.getTargetDirPath(), S3TestUtils.CUSTOM_MAP_OUTPUT_FILE_00001);
    }

    private String[] getArgsWithAsAvroDataFileOption(boolean withMapreduceOutputBasenameProperty) {
        ArgumentArrayBuilder builder = S3TestUtils.getArgumentArrayBuilderForS3UnitTestsWithFileFormatOption(this,
                s3CredentialGenerator,"as-avrodatafile");
        if (withMapreduceOutputBasenameProperty) {
            builder.withProperty(MAPREDUCE_OUTPUT_BASENAME_PROPERTY, S3TestUtils.MAPREDUCE_OUTPUT_BASENAME);
        }
        return builder.build();
    }

    private String[] getIncrementalAppendArgsWithAsAvroDataFileOption(boolean withMapreduceOutputBasenameProperty) {
        ArgumentArrayBuilder builder = S3TestUtils.getArgumentArrayBuilderForS3UnitTestsWithFileFormatOption(this,
                s3CredentialGenerator,"as-avrodatafile");
        builder = S3TestUtils.addIncrementalAppendImportArgs(builder);
        if (withMapreduceOutputBasenameProperty) {
            builder.withProperty(MAPREDUCE_OUTPUT_BASENAME_PROPERTY, S3TestUtils.MAPREDUCE_OUTPUT_BASENAME);
        }
        return builder.build();
    }
}