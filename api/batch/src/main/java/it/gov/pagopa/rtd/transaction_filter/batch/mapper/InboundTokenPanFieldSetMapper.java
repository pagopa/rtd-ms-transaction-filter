package it.gov.pagopa.rtd.transaction_filter.batch.mapper;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of {@link FieldSetMapper}, to be used for a reader
 * related to files containing {@link InboundTransaction} data
 */

@RequiredArgsConstructor
public class InboundTokenPanFieldSetMapper implements FieldSetMapper<InboundTokenPan> {


    /**
     *
     * @param fieldSet
     *          instance of FieldSet containing fields related to an {@link InboundTransaction}
     * @return instance of  {@link InboundTransaction}, mapped from a FieldSet
     * @throws BindException
     */
    @Override
    public InboundTokenPan mapFieldSet(@Nullable FieldSet fieldSet) throws BindException {

        if (fieldSet == null) {
            return null;
        }

        /*
           Building the {@link InboundTransaction} instance from the record data, defined within
           the {@link FieldSetMapper}, using the appropriate column id
         */

        return InboundTokenPan.builder()
                .tokenPan(fieldSet.readString("token_pan"))
                .circuitType(fieldSet.readString("circuit_type"))
                .par(fieldSet.readString("par"))
                .valid(true)
                .build();


    }

}
