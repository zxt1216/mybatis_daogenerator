import com.mysql.jdbc.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Sean
 * @date 2018-12-20
 * @description 优化版本，默认的文件地址在src/main/resources/file
 * @vesion 1.0
 */
public class Generator {
    //=============================TODO 需要配置的参数
    //数据库
    private static String scheme="cpc_auth";
    private static String url = "jdbc:mysql://127.0.0.1/"+scheme;
    private static String username = "username";
    private static String password = "password";
    //目标文件包名
    private static String basePackage = "com.virgo.finance.cpc.auth";
    private static String daoSubPackage = ".dao";

    //==============================默认参数==================================
    // 表名称
    private static HashMap<String, KeyValue> tables = new HashMap<>();
    //domain子包
    private static String domainSubPackage = ".domain";
    //生成文件后缀
    private static String daoPostfix = "Mapper";//Dao
    private static String domainPostfix = "";//Entity
    //自增主键,默认ID
    private static final String KEY = "ID";
    //不需要插入的字段的集合,即依赖数据自动生成  不包括ID
    private static final Set<String> unInsertColumns=new HashSet<>();
    //映射集合 key: mysql字段类型,value: 对应映射
    private static final Map<String,Mapping> mapperings=new HashMap<>();
    //默认转码,将字段转为驼峰,以下划线开头的字段,保留开头的下划线,且下划线后第一个字母小写
    private static final Boolean isHump=true;
    //domain需要额外引入的包
    private static final List<String> importsDomain=new ArrayList<>();
    //Mapper需要额外引入的包
    private static final List<String> importsMapper=new ArrayList<>();
    //domain需要的注解
    private static final List<String> annotations=new ArrayList<>();
    //domain需要实现的接口
    private static final List<String> implement=new ArrayList<>();
    //生成的Mapper和XML默认方法集合
    private static final Set<Method> methods=new HashSet<>();

    //=========================CONST===========================
    private static final String SEP = System.getProperty("line.separator");
    private static final char SPECTATOR = '_';
    private static final char SPECTATOR2 = '-';


    public static void main(String[] args) throws Exception {
        init();
    }

    //初始化表
    static {
        //================================= TODO 需要配置的参数
        tables.put("SYSTEM_DEPT", new KeyValue("DEPT_ID"));
        tables.put("SYSTEM_USER", new KeyValue("USER_ID"));
        tables.put("SYSTEM_ROLE", new KeyValue("ROLE_ID"));
        tables.put("SYSTEM_USER_ROLE", new KeyValue("USER_ROLE_ID"));
        tables.put("SYSTEM_MENU", new KeyValue("MENU_ID"));
        tables.put("SYSTEM_ROLE_MENU", new KeyValue("ROLE_MENU_ID"));
        tables.put("SYSTEM_RESOURCE", new KeyValue("RESOURCE_ID"));
        tables.put("SYSTEM_ROLE_RESOURCE", new KeyValue("ROLE_RESOURCE_ID"));
        tables.put("SYSTEM_ACTION", new KeyValue("ACTION_ID"));
        tables.put("SYSTEM_ROLE_ACTION", new KeyValue("ROLE_ACTION_ID"));
        tables.put("SYSTEM_LOG", new KeyValue("LOG_ID"));

        //========================================默认导入,注解,实现,方法==========================================
        importsDomain.add( "import lombok.Data;");
        importsDomain.add("import java.io.Serializable;");
        importsMapper.add("import java.util.Collection;");
        importsMapper.add("import org.apache.ibatis.annotations.Param;");
        annotations.add("@Data");
        implement.add("Serializable");
        unInsertColumns.add("GMT_CREATE");
        unInsertColumns.add("GMT_MODIFIED");


        methods.add(Method.GET);
        methods.add(Method.INSERT);
        methods.add(Method.BATCH_INSERT);
        methods.add(Method.DELETE);
        methods.add(Method.QUERY);
//        methods.add(Method.PAGE);
//        methods.add(Method.COUNT);
        methods.add(Method.UPDATE);
//        methods.add(Method.COVER);


        //==============================================CONST================================================
        //VARCHAR
        mapperings.put("varchar",new Mapping("varchar","String","java.lang.String","VARCHAR"));
        mapperings.put("char",new Mapping("char","Character","java.lang.Character","CHAR"));
        //数字
        mapperings.put("integer",new Mapping("integer","Integer","java.lang.Integer","INTEGER"));
        mapperings.put("int",new Mapping("int","Integer","java.lang.Integer","INTEGER"));
        mapperings.put("smallint",new Mapping("smallint","Integer","java.lang.Integer","SMALLINT"));
        mapperings.put("tinyint",new Mapping("tinyint","Byte","java.lang.Byte","TINYINT"));
        mapperings.put("mediumint",new Mapping("mediumint","Integer","java.lang.Integer","MEDIUMINT"));
        mapperings.put("bigint",new Mapping("bigint","Long","java.lang.Long","BIGINT"));
        mapperings.put("bit",new Mapping("bit","Boolean","java.lang.Boolean","BIT"));
        mapperings.put("boolean",new Mapping("boolean","Boolean","java.lang.Boolean","TINYINT"));
        mapperings.put("float",new Mapping("float","Float","java.lang.Float","FLOAT"));
        mapperings.put("double",new Mapping("double","Double","java.lang.Double","DOUBLE"));
        mapperings.put("decimal",new Mapping("decimal","BigDecimal","java.lang.BigDecimal","DECIMAL"));
        mapperings.put("numeric",new Mapping("numeric","BigDecimal","java.lang.BigDecimal","DECIMAL"));
        //日期
        mapperings.put("date",new Mapping("date","LocalDate","java.time.LocalDate","TIMESTAMP"));
        mapperings.put("time",new Mapping("time","LocalTime","java.time.LocalTime","TIME"));
        mapperings.put("datetime",new Mapping("datetime","LocalDateTime","java.time.LocalDateTime","TIMESTAMP"));
        mapperings.put("timestamp",new Mapping("timestamp","LocalDateTime","java.time.LocalDateTime","TIMESTAMP"));
        mapperings.put("year",new Mapping("year","LocalDate","java.lang.LocalDate","Year"));
        //TEXT
        mapperings.put("longtext",new Mapping("longtext","String","java.lang.String","VARCHAR"));
        mapperings.put("text",new Mapping("text","String","java.lang.String","VARCHAR"));
        mapperings.put("json",new Mapping("json","String","java.lang.String","VARCHAR"));
        mapperings.put("tinytext",new Mapping("tinytext","String","java.lang.String","VARCHAR"));
        mapperings.put("mediumtext",new Mapping("mediumtext","String","java.lang.String","MEDIUMTEXT"));
        //BLOB AND BINARY
        mapperings.put("blob",new Mapping("blob","byte[]","java.lang.Byte","BLOB"));
        mapperings.put("longblob",new Mapping("longblob","byte[]","java.lang.Byte","LONGBLOB"));
        mapperings.put("binary",new Mapping("binary","byte[]","java.lang.Byte","BINARY"));
        mapperings.put("varbinary",new Mapping("varbinary","byte[]","java.lang.Byte","VARBINARY"));
        mapperings.put("tinyblob",new Mapping("tinyblob","byte[]","java.lang.Byte","TINYBLOB"));
        mapperings.put("mediumblob",new Mapping("mediumblob","byte[]","java.lang.Byte","MEDIUMBLOB"));
    }


    public static void init() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        File file = new File("src/main/resources/file");
        file.mkdir();
        File file2 = new File("src/main/resources/file/dao");
        file2.mkdir();
        File file3 = new File("src/main/resources/file/domain");
        file3.mkdir();
        File file4 = new File("src/main/resources/file/mapper");
        file4.mkdir();
        Connection conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        generate(st);
        st.close();
        conn.close();
    }

    private static void generate(Statement st) throws Exception {
        for (String table : tables.keySet()) {
            System.out.println(table);
            //表内容
            List<String> columns = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<String> columnComments = new ArrayList<>();
            //表注释
            String tableComment = tableSlash(table,columns,types,columnComments,st);
            //生成domain
            generateDomain(table,columns,types,columnComments,tableComment);
            //生成mapper
            generateMapper(table,tableComment);
            //生成Xml
            generateXML(table,columns,types);
        }
    }
    private static String tableSlash(String tableName, List<String> columns, List<String> types, List<String> comments, Statement st)throws Exception{
        String tableComment="";
        ResultSet rs = st.executeQuery("select COLUMN_NAME , DATA_TYPE , COLUMN_COMMENT from information_schema.COLUMNS where table_name='" + tableName + "' and TABLE_SCHEMA='"+scheme+"'");
        while (rs.next()) {
            String column=rs.getString("COLUMN_NAME");
            String type=rs.getString("DATA_TYPE").toLowerCase();
            columns.add(column);
            if(column.equals(tables.get(tableName).getKey())){
                tables.get(tableName).setValue(type);
            }
            types.add(type);
            comments.add(rs.getString("COLUMN_COMMENT"));
        }
        ResultSet rs2 = st.executeQuery("select TABLE_COMMENT from information_schema.tables where table_name='" + tableName + "'");
        while (rs2.next()) {
            tableComment = rs2.getString("TABLE_COMMENT");
        }
        rs.close();
        rs2.close();
        return tableComment;
    }
    private static void generateDomain(String tableName,List<String> columns,List<String> types,List<String> comments,String comment4Table) throws Exception{
        String entityName = col2Object(tableName) + domainPostfix;

        StringBuilder sb=new StringBuilder();
        sb.append("package ").append(basePackage).append(daoSubPackage).append(domainSubPackage).append(";").append(SEP);

        for (String s : new HashSet<>(types)) {
            if(null==mapperings.get(s)){
                throw new RuntimeException("出现不支持的Mysql数据类型,请求人工介入");
            }
            if(mapperings.get(s).javaImport.contains("java.lang")){
                continue;
            }
            sb.append(SEP).append("import ").append(mapperings.get(s).javaImport).append(";");
        }
        for(String s:importsDomain){
            sb.append(SEP).append(s);
        }
        sb.append(SEP)
          .append(SEP).append("/**")
          .append(SEP).append(" * ").append(comment4Table).append(" Entity")
          .append(SEP).append(" * @author Sean")
          .append(SEP).append(" * @date ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .append(SEP).append(" */");
        for(String s:annotations){
            sb.append(SEP).append(s);
        }
        sb.append(SEP).append("public class ").append(entityName).append(" implements ");
        for(String s:implement){
            sb.append(s).append(",");
        }
        if(implement.size()>0) {
            sb.deleteCharAt(sb.length()-1);
        }
        sb.append("{");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append(comments.get(i))
                    .append(SEP).append("    */")
                    .append(SEP).append("    private ").append(mapperings.get(types.get(i)).javaType).append(" ").append(col2Field(columns.get(i))).append(";").append(SEP);
        }
        sb.append(SEP).append("}");

        File file = new File("src/main/resources/file/domain/" + entityName + ".java");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(sb.toString());
        bw.close();
    }

    private static void generateMapper(String tableName,String comment4Table) throws Exception{
        String entityName = col2Object(tableName) + domainPostfix;
        String daoName = col2Object(tableName) + daoPostfix;
        String entityRefName = basePackage + daoSubPackage+domainSubPackage + "." +entityName;


        StringBuilder sb=new StringBuilder();
        sb.append("package ").append(basePackage).append(daoSubPackage).append(";").append(SEP);

//        for (String s : new HashSet<>(types)) {
//            if(null==mapperings.get(s)){
//                throw new RuntimeException("出现不支持的Mysql数据类型,请求人工介入");
//            }
//            sb.append(SEP).append("import ").append(mapperings.get(s).javaImport).append(";");
//        }
        if(!mapperings.get(tables.get(tableName).getValue()).javaImport.startsWith("java.lang")){
            sb.append(SEP).append("import ").append(mapperings.get(tables.get(tableName).getValue()).javaImport).append(";");
        }

        for(String s:importsMapper){
            sb.append(SEP).append(s);
        }
        sb.append(SEP).append("import ").append(entityRefName).append(";").append(SEP)
          .append(SEP).append("/**")
          .append(SEP).append(" * ").append(comment4Table).append(" Dao")
          .append(SEP).append(" * @author Sean")
          .append(SEP).append(" * @date ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .append(SEP).append(" */")
          .append(SEP).append("public interface ").append(daoName).append("{");

        //主键Field
        String field=col2Field(tables.get(tableName).getKey());
        if(methods.contains(Method.GET)) {
            sb.append(SEP).append("    /**")
              .append(SEP).append("    * ").append("根据主键查询")
              .append(SEP).append("    * ")
              .append(SEP).append("    * ").append("@param ").append(field).append(" 主键值")
              .append(SEP).append("    * ").append("@return 根据主键查询到的对象")
              .append(SEP).append("    */")
              .append(SEP).append("    ").append(entityName).append(" get(").append(mapperings.get(tables.get(tableName).getValue()).javaType).append(" ").append(field).append(");").append(SEP);
        }
        if(methods.contains(Method.UPDATE)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据主键更新[更新非空字段]")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 待更新的对象")
                    .append(SEP).append("    * ").append("@return 更新where条件影响条数")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int update(").append(entityName).append(" entity);").append(SEP);
        }
        if(methods.contains(Method.COUNT)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据条件统计")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 条件对象")
                    .append(SEP).append("    * ").append("@return 统计条数")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int count(").append(entityName).append(" entity);").append(SEP);
        }
        if(methods.contains(Method.COVER)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据主键更新[覆盖]")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 待更新的对象")
                    .append(SEP).append("    * ").append("@return 更新where条件影响条数")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int cover(").append(entityName).append(" entity);").append(SEP);
        }
        if(methods.contains(Method.INSERT)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("插入一条数据")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 待插入的对象")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int insert(").append(entityName).append(" entity);").append(SEP);
        }
        if(methods.contains(Method.QUERY)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据条件查询")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 条件对象")
                    .append(SEP).append("    * ").append("@return 条件符合数据")
                    .append(SEP).append("    */")
                    .append(SEP).append("    Collection<").append(entityName).append("> query(").append(entityName).append(" entity);").append(SEP);
        }
        if(methods.contains(Method.PAGE)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据条件分页查询")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entity 条件对象")
                    .append(SEP).append("    * ").append("@param start 起始条数")
                    .append(SEP).append("    * ").append("@param size 条数")
                    .append(SEP).append("    * ").append("@return 条件符合数据")
                    .append(SEP).append("    */")
                    .append(SEP).append("    List<").append(entityName).append("> page(@Param(\"entity\")").append(entityName).append(" entity")
                    .append(",@Param(\"start\")").append("Integer").append(" start")
                    .append(",@Param(\"size\")").append("Integer").append(" size);").append(SEP);
        }
        if(methods.contains(Method.DELETE)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("根据主键删除")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param ").append(field).append(" 主键值")
                    .append(SEP).append("    * ").append("@return 删除条数")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int").append(" delete(").append(mapperings.get(tables.get(tableName).getValue()).javaType).append(" ").append(field).append(");").append(SEP);
        }
        if(methods.contains(Method.BATCH_INSERT)) {
            sb.append(SEP).append("    /**")
                    .append(SEP).append("    * ").append("批量插入")
                    .append(SEP).append("    * ")
                    .append(SEP).append("    * ").append("@param entities 待更新的对象")
                    .append(SEP).append("    */")
                    .append(SEP).append("    int batchInsert(@Param(\"entities\") Collection<").append(entityName).append("> entities);").append(SEP)
                    .append(SEP).append("}");
        }

        File file2 = new File("src/main/resources/file/dao/" + daoName + ".java");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
        bw.write(sb.toString());
        bw.close();
    }

    private static void generateXML(String table,List<String> columns,List<String> types) throws Exception{
        String entityName = col2Object(table) + domainPostfix;
        String daoName = col2Object(table) + daoPostfix;
        String entityRefName = basePackage +daoSubPackage+ domainSubPackage + "." +entityName;
        String daoRefName = basePackage +  daoSubPackage + "." +daoName;


        StringBuilder sb=new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        sb.append(SEP).append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >");
        sb.append(SEP).append("<mapper namespace=\"").append(daoRefName).append("\">");
        //resultMap
        sb.append(SEP).append("    <resultMap id=\"baseResult\" type=\"").append(entityRefName).append("\">");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(SEP).append("        <result column=\"").append(columns.get(i)).append("\" property=\"").append(col2Field(columns.get(i)))
              .append("\" jdbcType=\"").append(mapperings.get(types.get(i)).jdbcType).append("\"/>");
        }
        sb.append(SEP).append("    </resultMap>");
        sb.append(SEP).append("    <!-- 基础字段 -->")
          .append(SEP).append("    <sql id=\"baseColumns\">")
          .append(SEP).append("        ").append("`").append(KEY).append("`").append(",")
          .append(SEP).append("        <include refid=\"insertColumns\"/>")
          .append(",");
        for(String col:unInsertColumns) {
          sb.append(SEP).append("        `").append(col).append("`,");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(SEP).append("    </sql>");
        sb.append(SEP).append("    <!-- Insert字段 -->")
          .append(SEP).append("    <sql id=\"insertColumns\">").append(SEP);

        StringBuilder build2 = new StringBuilder();
        build2.append("        ");
        String post=SEP+"        ";
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equals(KEY) || unInsertColumns.contains(columns.get(i))) {
                continue;
            } else {
                build2.append("`").append(columns.get(i)).append("`,");
            }
            if (i % 5 == 0) {
                build2.append(post);
            }
        }
        String insert=build2.toString();
        //处理差异
        if(insert.endsWith(post)){
            insert=insert.substring(0,insert.length()-post.length());
        }
        sb.append(insert.substring(0,insert.length()-1))
          .append(SEP).append("    </sql>");
        if(methods.contains(Method.GET)) {
            sb.append(SEP).append("    <!-- 根据主键查询一行数据 -->")
              .append(SEP).append("    <select id=\"get\" parameterType=\"").append(mapperings.get(tables.get(table).getValue()).javaImport).append("\" resultMap=\"baseResult\">")
              .append(SEP).append("        SELECT")
              .append(SEP).append("        <include refid=\"baseColumns\"/>")
              .append(SEP).append("        FROM ").append(table)
              .append(SEP).append("        WHERE `").append(tables.get(table).getKey()).append("` = #{").append(col2Field(tables.get(table).getKey())).append(",jdbcType=").append(mapperings.get(tables.get(table).getValue()).jdbcType).append("}")
              .append(SEP).append("    </select>");
        }
        if(methods.contains(Method.UPDATE)) {
            sb.append(SEP).append("    <!-- 根据主键更新非空字段 -->")
              .append(SEP).append("    <update id=\"update\" parameterType=\"").append(entityRefName).append("\">")
              .append(SEP).append("        UPDATE ").append(table)
              .append(SEP).append("        <set>");
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equals(tables.get(table).getKey()) || unInsertColumns.contains(columns.get(i)) || columns.get(i).equals(KEY)) {
                    continue;
                } else {
                    if (mapperings.get(types.get(i)).javaType.equals("String")) {
                        sb.append(SEP).append("            <if test=\"").append(col2Field(columns.get(i))).append(" != null and ").append(col2Field(columns.get(i))).append(" != '' \" >")
                          .append(SEP).append("                ").append("`").append(columns.get(i)).append("` = #{").append(col2Field(columns.get(i))).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("},")
                          .append(SEP).append("            </if>");
                    } else {
                        sb.append(SEP).append("            <if test=\"").append(col2Field(columns.get(i))).append(" != null\" >")
                          .append(SEP).append("                ").append("`").append(columns.get(i)).append("` = #{").append(col2Field(columns.get(i))).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("},")
                          .append(SEP).append("            </if>");
                    }
                }
            }
            sb.append(SEP).append("        </set>");
            sb.append(SEP).append("        WHERE `").append(tables.get(table).getKey()).append("` = #{").append(col2Field(tables.get(table).getKey())).append(",jdbcType=").append(mapperings.get(tables.get(table).getValue()).jdbcType).append("}")
              .append(SEP).append("    </update>");
        }
        if(methods.contains(Method.COVER)) {
            sb.append(SEP).append("    <!-- 根据主键更新全部字段 -->")
              .append(SEP).append("    <update id=\"cover\" parameterType=\"").append(entityRefName).append("\">")
              .append(SEP).append("        UPDATE ").append(table)
              .append(SEP).append("        SET ");
            StringBuilder build = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equals(tables.get(table).getKey()) || unInsertColumns.contains(columns.get(i))) {
                    continue;
                } else {
                    build.append(SEP).append("            `").append(columns.get(i)).append("` = #{").append(col2Field(columns.get(i))).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("},");
                }
            }
            sb.append(build.substring(0, build.length() - 1));
            sb.append(SEP).append("        WHERE `").append(tables.get(table).getKey()).append("` = #{").append(col2Field(tables.get(table).getKey())).append(",jdbcType=").append(mapperings.get(tables.get(table).getValue()).jdbcType).append("}")
              .append(SEP).append("    </update>");
        }
        if(methods.contains(Method.COUNT)) {
            sb.append(SEP).append("    <!-- 统计数量 -->")
              .append(SEP).append("    <select id=\"count\" parameterType=\"").append(entityRefName).append("\" resultType=\"java.lang.Integer\">")
              .append(SEP).append("        SELECT count(*) FROM ").append(table)
              .append(SEP).append("        <where>");
            for (int i = 0; i < columns.size(); i++) {
                String field=col2Field(columns.get(i));
                if (mapperings.get(types.get(i)).javaType.equals("String")) {
                    sb.append(SEP).append("            <if test=\"").append(field).append(" != null and ").append(field).append(" != '' \" >")
                      .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                      .append(SEP).append("            </if>");
                } else {
                    sb.append(SEP).append("            <if test=\"").append(field).append(" != null\" >")
                      .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                      .append(SEP).append("            </if>");
                }
            }
            sb.append(SEP).append("        </where>")
                    .append(SEP).append("    </select>");
        }
        if(methods.contains(Method.INSERT)) {
            sb.append(SEP).append("    <!-- 插入数据 -->")
              .append(SEP).append("    <insert id=\"insert\" parameterType=\"").append(entityRefName).append("\" useGeneratedKeys=\"true\" keyProperty=\"").append(col2Field(KEY)).append("\">")
              .append(SEP).append("        INSERT INTO ").append(table).append("(<include refid=\"insertColumns\"/>) VALUES(");
            StringBuilder build3 = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equals(KEY) || unInsertColumns.contains(columns.get(i))) {
                    continue;
                } else {
                    build3.append(SEP).append("            #{").append(col2Field(columns.get(i))).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("},");
                }
            }
            sb.append(build3.substring(0, build3.length() - 1)).append(SEP).append("        )");
            sb.append(SEP).append("    </insert>");
        }
        if(methods.contains(Method.BATCH_INSERT)) {
            sb.append(SEP).append("    <!-- 批量插入数据 -->")
              .append(SEP).append("    <insert id=\"batchInsert\" parameterType=\"java.util.Collection\" useGeneratedKeys=\"true\" keyProperty=\"").append(col2Field(KEY)).append("\">")
              .append(SEP).append("        INSERT INTO ").append(table).append("(<include refid=\"insertColumns\"/>) VALUES ")
              .append(SEP).append("        <foreach collection=\"entities\" item=\"item\" separator=\",\">(");
            StringBuilder build4 = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equals(KEY) || unInsertColumns.contains(columns.get(i))) {
                    continue;
                } else {
                    build4.append(SEP).append("            #{item.").append(col2Field(columns.get(i))).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("},");
                }
            }
            sb.append(build4.substring(0, build4.length() - 1));
            sb.append(SEP).append("        )</foreach>")
              .append(SEP).append("    </insert>");
        }
        if(methods.contains(Method.QUERY)) {
            sb.append(SEP).append("    <!-- 条件查询 -->")
              .append(SEP).append("    <select id=\"query\" parameterType=\"").append(entityRefName).append("\" resultMap=\"baseResult\">")
              .append(SEP).append("        SELECT <include refid=\"baseColumns\" /> FROM ").append(table)
              .append(SEP).append("        <where>");
            for (int i = 0; i < columns.size(); i++) {
                String field=col2Field(columns.get(i));
                if (mapperings.get(types.get(i)).javaType.equals("String")) {
                    sb.append(SEP).append("            <if test=\"").append(field).append(" != null and ").append(field).append(" != '' \" >")
                      .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                      .append(SEP).append("            </if>");
                } else {
                    sb.append(SEP).append("            <if test=\"").append(field).append(" != null\" >")
                      .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                      .append(SEP).append("            </if>");
                }
            }
            sb.append(SEP).append("        </where>")
              .append(SEP).append("    </select>");
        }
        if(methods.contains(Method.PAGE)) {
            sb.append(SEP).append("    <!-- 条件分页查询 -->")
                    .append(SEP).append("    <select id=\"page\" resultMap=\"baseResult\">")
                    .append(SEP).append("        SELECT <include refid=\"baseColumns\" /> FROM ").append(table)
                    .append(SEP).append("        <where>");
            for (int i = 0; i < columns.size(); i++) {
                String field=col2Field(columns.get(i));
                if (mapperings.get(types.get(i)).javaType.equals("String")) {
                    sb.append(SEP).append("            <if test=\"entity.").append(field).append(" != null and entity.").append(field).append(" != '' \" >")
                            .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{entity.").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                            .append(SEP).append("            </if>");
                } else {
                    sb.append(SEP).append("            <if test=\"entity.").append(field).append(" != null\" >")
                            .append(SEP).append("            AND `").append(columns.get(i)).append("` = #{entity.").append(field).append(",jdbcType=").append(mapperings.get(types.get(i)).jdbcType).append("}")
                            .append(SEP).append("            </if>");
                }
            }
            sb.append(SEP).append("        </where>")
                    .append(SEP).append("        limit #{start},#{size}")
                    .append(SEP).append("    </select>");
        }
        if(methods.contains(Method.DELETE)) {
            sb.append(SEP).append("    <!-- 根据主键删除一行数据 -->")
                    .append(SEP).append("    <delete id=\"delete\" parameterType=\"").append(mapperings.get(tables.get(table).getValue()).javaImport).append("\">")
                    .append(SEP).append("        DELETE FROM ").append(table)
                    .append(SEP).append("        WHERE `").append(tables.get(table).getKey()).append("` = #{").append(col2Field(tables.get(table).getKey())).append(",jdbcType=").append(mapperings.get(tables.get(table).getValue()).jdbcType).append("}")
                    .append(SEP).append("    </delete>");

        }
        sb.append(SEP).append("</mapper>");
        File file3 = new File("src/main/resources/file/mapper/" + daoName + ".xml");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file3));
        bw.write(sb.toString());
        bw.close();
    }


    /**
     * 数据库字段 转换成 java 字段
     * @param column
     * @return
     */
    private static String col2Field(String column){
        if(StringUtils.isNullOrEmpty(column))return "";
        if(!isHump){
            column=column.replaceAll("-","_");
            return column;
        }
        StringBuilder  sb=new StringBuilder();
        char[] chars = column.toCharArray();
        int j=0;
        while(chars[j]==SPECTATOR || chars[j]==SPECTATOR2){
            sb.append(SPECTATOR);
            j++;
        }
       translate(chars,j,sb,false);
        return sb.toString();
    }

    /**
     * 将表明转换成对象名称
     * @param column
     * @return
     */
    private static String col2Object(String column){
        if(StringUtils.isNullOrEmpty(column))return "";
        StringBuilder  sb=new StringBuilder();
        char[] chars = column.toCharArray();
        int j=0;
        while(chars[j]==SPECTATOR || chars[j]==SPECTATOR2){
            j++;
        }
        translate(chars,j,sb,true);
        return sb.toString();
    }

    /**
     * 名称转换
     * @param chars
     * @param begin
     * @param sb
     * @param isUpper
     */
    private static void translate(char[] chars,int begin,StringBuilder  sb,boolean isUpper){
        for (int i = begin; i < chars.length; i++) {
            if (chars[i] == SPECTATOR || chars[i] == SPECTATOR2) {
                isUpper = true;
            } else {
                if (isUpper) {
                    sb.append(Character.toUpperCase(chars[i]));
                    isUpper = false;
                } else {
                    sb.append(Character.toLowerCase(chars[i]));
                }
            }
        }
    }

    @Data
    public static class KeyValue {
        private String key;
        private String value;
        public KeyValue(String key) {
            this.key = key;
        }
        public KeyValue() {
            this.key = KEY;
        }
    }
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mapping{
        //mysql 字段类型
        private String sqlType;
        //对应的 java 类型(简写)
        private String javaType;
        //对应的导入
        private String javaImport;
        //对应的 jdbcType
        private String jdbcType;
    }

    public enum Method{
        GET,
        INSERT,
        BATCH_INSERT,
        QUERY,
        PAGE,
        UPDATE,
        COVER,
        DELETE,
        COUNT,
        ;
    }
}
