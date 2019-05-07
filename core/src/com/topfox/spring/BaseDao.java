package com.topfox.spring;

import com.topfox.common.DataQTO;
import com.topfox.common.DataDTO;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;

public interface BaseDao<QTO extends DataQTO,DTO extends DataDTO> {
	@InsertProvider(type = ProviderSql.class, method = "insertBySql")
	int executeByInsertSql(String sql);

	@DeleteProvider(type = ProviderSql.class, method = "deleteBySql")
	int executeByDeleteSql(String sql);

	@UpdateProvider(type = ProviderSql.class, method = "updateBySql")
	int executeByUpdateSql(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	List<Map<String, Object>> selectBySql(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	int selectForInteger(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	List<DTO> selectForDTO(String sql);

//	List<DTO> list(QTO qto);
//	List<DTO> listCount(QTO qto);

	List<DTO> list(Map<String, Object> qto);
	List<DTO> listCount(Map<String, Object> qto);

	List<DTO> query1(Map<String, Object> qto);
	int queryCount1(Map<String, Object> qto);

	List<DTO> query2(Map<String, Object> qto);
	int queryCount2(Map<String, Object> qto);

	List<DTO> query3(Map<String, Object> qto);
	int queryCount3(Map<String, Object> qto);

	List<DTO> query4(Map<String, Object> qto);
	int queryCount4(Map<String, Object> qto);

	List<DTO> query5(Map<String, Object> qto);
	int queryCount5(Map<String, Object> qto);

	List<DTO> query6(Map<String, Object> qto);
	int queryCount6(Map<String, Object> qto);

	List<DTO> query7(Map<String, Object> qto);
	int queryCount7(Map<String, Object> qto);

	List<DTO> query8(Map<String, Object> qto);
	int queryCount8(Map<String, Object> qto);

	List<DTO> query9(Map<String, Object> qto);
	int queryCount9(Map<String, Object> qto);

}
