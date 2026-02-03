package com.datacentric.utils.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonType implements UserType<JSON> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(JsonType.class);

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<JSON> returnedClass() {

        return JSON.class;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, JSON value, int column,
            SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            ps.setNull(column, Types.VARCHAR);
        } else {
            try {
                String dbValue = mapper.writeValueAsString(value);
                ps.setString(column, dbValue);
            } catch (JsonProcessingException e) {
                throw new SQLException("Failure converting json to string", e);
            }
        }
    }

    @Override
    public JSON nullSafeGet(ResultSet rs, int column, SharedSessionContractImplementor session,
            Object owner)
            throws SQLException {
        String dbValue = rs.getString(column);
        if (dbValue == null) {
            return null;
        }

        try {
            return mapper.readValue(dbValue, new TypeReference<JSON>() {
            });
        } catch (JsonProcessingException e) {
            throw new SQLException("Invalid value", e);
        }
    }

    @Override
    public boolean equals(JSON a, JSON b) {
        return a != null && a.equals(b);
    }

    @Override
    public int hashCode(JSON a) {
        if (a == null) {
            return 0;
        }

        return a.hashCode();
    }

    @Override
    public Serializable disassemble(JSON a) {
        return a;
    }

    @Override
    public JSON assemble(Serializable a, Object owner) {
        return (JSON) a;
    }

    @Override
    public JSON deepCopy(JSON a) {
        if (a == null) {
            return a;
        }
        return a.deepCopy();
    }
}