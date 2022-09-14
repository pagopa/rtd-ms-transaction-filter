package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.model.RawInputLine;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Custom implementation of {@link FlatFileItemReader}, the source code is replicated
 * from the original class, and modified to manage proper record line awareness
 */
@Slf4j
public class TransactionFlatFileItemReader  extends FlatFileItemReader<InboundTransaction> {
    public static final String DEFAULT_CHARSET = Charset.defaultCharset().name();
    protected static final String[] DEFAULT_COMMENT_PREFIXES = new String[]{"#"};
    private RecordSeparatorPolicy recordSeparatorPolicy = new SimpleRecordSeparatorPolicy();
    private Resource resource;
    private BufferedReader reader;
    private int lineCount = 0;
    private String[] commentsPrefix;
    private boolean noInput;
    private String encoding;
    private LineMapper<InboundTransaction> lineMapper;
    private int linesToSkip;
    private LineCallbackHandler skippedLinesCallback;
    private boolean strict;
    private BufferedReaderFactory bufferedReaderFactory;

    public TransactionFlatFileItemReader() {
        this.commentsPrefix = DEFAULT_COMMENT_PREFIXES;
        this.noInput = false;
        this.encoding = DEFAULT_CHARSET;
        this.linesToSkip = 0;
        this.strict = true;
        this.bufferedReaderFactory = new DefaultBufferedReaderFactory();
        this.setName(ClassUtils.getShortName(FlatFileItemReader.class));
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public void setSkippedLinesCallback(LineCallbackHandler skippedLinesCallback) {
        this.skippedLinesCallback = skippedLinesCallback;
    }

    @Override
    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    @Override
    public void setLineMapper(LineMapper<InboundTransaction> lineMapper) {
        this.lineMapper = lineMapper;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void setBufferedReaderFactory(BufferedReaderFactory bufferedReaderFactory) {
        this.bufferedReaderFactory = bufferedReaderFactory;
    }

    @Override
    public void setComments(String[] comments) {
        this.commentsPrefix = new String[comments.length];
        System.arraycopy(comments, 0, this.commentsPrefix, 0, comments.length);
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
        this.recordSeparatorPolicy = recordSeparatorPolicy;
    }

    @Override
    @Nullable
    protected InboundTransaction doRead() {
        if (this.noInput) {
            return null;
        } else {
            RawInputLine line = this.readNextLine();
            if (line == null) {
                return null;
            } else {
                int innerCount = line.getLineNumber();
                try {

                    return this.lineMapper.mapLine(
                            line.getContent(),
                            line.getLineNumber());

                } catch (Exception ex) {
                    throw new FlatFileParseException("Parsing error at line: " + innerCount + " in resource=[" +
                            this.resource.getDescription() + "]" , ex, line.getContent(), innerCount);
                }
            }
        }
    }

    @Nullable
    private RawInputLine readNextLine() {
        if (this.reader == null) {
            throw new ReaderNotOpenException("Reader must be open before it can be read.");
        } else {
            String lineContent = null;
            int count = 0;

            try {
                lineContent = this.reader.readLine();
                if (lineContent == null) {
                    return null;
                } else {

                    synchronized (this) {
                        ++this.lineCount;

                        while (this.isComment(lineContent)) {
                            lineContent = this.reader.readLine();
                            if (lineContent == null) {
                                return null;
                            }

                            ++this.lineCount;
                        }
                        count = this.lineCount;
                    }

                    lineContent = this.applyCustomRecordSeparatorPolicy(lineContent);
                    return RawInputLine.builder().lineNumber(count).content(lineContent).build();
                }
            } catch (IOException ex) {
                this.noInput = true;
                throw new NonTransientFlatFileException("Unable to read from resource: [" + this.resource + "]",
                        ex, Optional.ofNullable(lineContent).orElse(""), this.lineCount);
            }
        }
    }

    @Override
    protected boolean isComment(String line) {
        String[] commentsArray = this.commentsPrefix;

        for (String prefix : commentsArray) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doClose() throws Exception {
        this.lineCount = 0;
        if (this.reader != null) {
            this.reader.close();
        }

    }

    @Override
    protected void doOpen() throws Exception {
        Assert.notNull(this.resource, "Input resource must be set");
        Assert.notNull(this.recordSeparatorPolicy, "RecordSeparatorPolicy must be set");
        this.noInput = true;
        if (!this.resource.exists()) {
            if (this.strict) {
                throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode): " +
                        this.resource);
            } else {
                log.warn("Input resource does not exist " + this.resource.getDescription());
            }
        } else if (!this.resource.isReadable()) {
            if (this.strict) {
                throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode): " +
                        this.resource);
            } else {
                log.warn("Input resource is not readable " + this.resource.getDescription());
            }
        } else {
            this.reader = this.bufferedReaderFactory.create(this.resource, this.encoding);

            for(int i = 0; i < this.linesToSkip; ++i) {
                RawInputLine line = this.readNextLine();
                if (this.skippedLinesCallback != null && line != null) {
                    this.skippedLinesCallback.handleLine(line.toString());
                }
            }

            this.noInput = false;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.lineMapper, "LineMapper is required");
    }

    @Override
    protected void jumpToItem(int itemIndex) {
        for(int i = 0; i < itemIndex; ++i) {
            this.readNextLine();
        }

    }

    private String applyCustomRecordSeparatorPolicy(String line) throws IOException {
        String currentRecord;
        for(currentRecord = line; line != null && !this.recordSeparatorPolicy.isEndOfRecord(currentRecord); currentRecord =
                this.recordSeparatorPolicy.preProcess(currentRecord) + line) {
            line = this.reader.readLine();
            if (line == null) {
                if (StringUtils.hasText(currentRecord)) {
                    throw new FlatFileParseException("Unexpected end of file before record complete",
                            currentRecord, this.lineCount);
                }
                break;
            }

            ++this.lineCount;
        }

        return this.recordSeparatorPolicy.postProcess(Optional.ofNullable(currentRecord).orElse(""));
    }
}
