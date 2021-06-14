package it.gov.pagopa.rtd.transaction_filter.batch.mapper;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import lombok.Data;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
* Custom implementation of {@link LineMapper}, to be used for adding the lineNumber and filename to the
 * mapped record
*/
@Data
public class InboundTokenPanLineAwareMapper<T> implements LineMapper<InboundTokenPan>, InitializingBean {

    private LineTokenizer tokenizer;

    private FieldSetMapper<InboundTokenPan> fieldSetMapper;

    private String filename;


    public InboundTokenPan mapLine(String line, int lineNumber) throws Exception {
        try{
            InboundTokenPan inboundTokenPan = fieldSetMapper.mapFieldSet(tokenizer.tokenize(line));
            inboundTokenPan.setLineNumber(lineNumber);
            inboundTokenPan.setFilename(filename);
            return inboundTokenPan;
        }
        catch(Exception ex){
            throw new FlatFileParseException("Parsing error at line: " + lineNumber, ex, line, lineNumber);
        }
    }

    public void afterPropertiesSet() {
        Assert.notNull(tokenizer, "The LineTokenizer must be set");
        Assert.notNull(fieldSetMapper, "The FieldSetMapper must be set");
    }

}