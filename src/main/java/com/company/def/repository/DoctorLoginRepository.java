package com.cephx.def.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class DoctorLoginRepository {
    private static final Logger logger = LogManager.getLogger(DoctorLoginRepository.class);
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public int getLoginCountWithVersion(long doctorId, String version) {
        String sql = "SELECT COUNT(*) FROM doctor_logins WHERE doctor_id = ? AND cephx_version = ?";
        return jdbcTemplate.queryForObject(sql, new Object[] {doctorId, version} ,Integer.class);
    }

    public int getTotalCountLogins(long doctorId) {
        String sql = "SELECT COUNT(*) FROM doctor_logins WHERE doctor_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[] {doctorId} ,Integer.class);
    }

    public void insert(final long doctorId, final String cephxVersion, final String ip) {
        final String sql = "INSERT INTO doctor_logins (doctor_id, cephx_version, ip) VALUES (?, ?, ?)";
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                final PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, doctorId);
                ps.setString(2, cephxVersion);
                ps.setString(3, ip);
                return ps;
            }
        });
    }

}
