package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

import slick.lifted.ProvenShape
import slick.lifted.PrimaryKey
import com.github.j5ik2o.threadWeaver.adaptor.dao._

<#assign softDelete=false>
trait ${className}Component extends SlickDaoSupport {

import profile.api._

case class ${className}RecordImpl(
<#list primaryKeys as primaryKey>
    ${primaryKey.propertyName}: ${primaryKey.propertyTypeName}<#if primaryKey_has_next>,</#if></#list><#if primaryKeys?has_content>,</#if>
<#list columns as column>
    <#if column.columnName == "deleted">
        <#assign softDelete=true>
    </#if>
    <#if column.nullable>    ${column.propertyName}: Option[${column.propertyTypeName}]<#if column_has_next>,</#if>
    <#else>    ${column.propertyName}: ${column.propertyTypeName}<#if column_has_next>,</#if>
    </#if>
</#list>
) extends <#if softDelete == false>Record<#else>SoftDeletableRecord</#if> with ${className}Record

case class ${className}s(tag: Tag) extends TableBase[${className}RecordImpl](tag, "${tableName}")<#if softDelete == true> with SoftDeletableTableSupport[${className}RecordImpl]</#if> {
<#list primaryKeys as primaryKey>
    def ${primaryKey.propertyName}: Rep[${primaryKey.propertyTypeName}] = column[${primaryKey.propertyTypeName}]("${primaryKey.columnName}")
</#list>
<#list columns as column>
    <#if column.nullable>
        def ${column.propertyName}: Rep[Option[${column.propertyTypeName}]] = column[Option[${column.propertyTypeName}]]("${column.columnName}")
    <#else>
        def ${column.propertyName}: Rep[${column.propertyTypeName}] = column[${column.propertyTypeName}]("${column.columnName}")
    </#if>
</#list>
def pk: PrimaryKey  = primaryKey("pk", (<#list primaryKeys as primaryKey>${primaryKey.propertyName}<#if primaryKey_has_next>,</#if></#list>))
override def * : ProvenShape[${className}RecordImpl] = (<#list primaryKeys as primaryKey>${primaryKey.propertyName}<#if primaryKey_has_next>,</#if></#list><#if primaryKeys?has_content>,</#if><#list columns as column>${column.propertyName}<#if column_has_next>,</#if></#list>) <> (${className}RecordImpl.tupled, ${className}RecordImpl.unapply)
}

object ${className}Dao extends TableQuery(${className}s)

}
