package com.zachary.delivery_system.typehandler;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresUuidTypeHandlerTest {

    private final PostgresUuidTypeHandler typeHandler =
            new PostgresUuidTypeHandler();

    @Test
    void setParameter_writesUuidAsPostgresOtherType() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID id = UUID.fromString(
                "624b8217-ff53-43ac-bbf0-8229f78f060c"
        );

        typeHandler.setParameter(statement, 1, id, JdbcType.OTHER);

        verify(statement).setObject(1, id, Types.OTHER);
    }

    @Test
    void getResult_readsNativePostgresUuid() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID id = UUID.fromString(
                "624b8217-ff53-43ac-bbf0-8229f78f060c"
        );
        when(resultSet.getObject("id")).thenReturn(id);

        UUID result = typeHandler.getResult(resultSet, "id");

        assertEquals(id, result);
    }
}
