package com.asiainfo.ocdc.streaming.impl

import com.asiainfo.ocdc.streaming._
import org.apache.spark.streaming.dstream.DStream
import com.asiainfo.ocdc.streaming.tools.HbaseTable
import org.apache.hadoop.hbase.util.Bytes
import scala.xml.Node
import com.asiainfo.ocdc.streaming.tools.HbaseTable._

class ExampleStep extends StreamingStep{

  def onStep(step:Node,inStream:DStream[Array[String]]):DStream[Array[String]]={

    val delim = ","
    var result = inStream
    val Input = (step \ "Input").text.toString.split(delim)
    val HBaseTable = (step \ "HBaseTable").text.toString
    val HBaseKey = (step \ "HBaseKey").text.toString.split(delim)
    val output = (step \ "output").text.toString.split(delim)
    val HBaseValue = (step \ "HBaseValue").text.toString
    val where = (step \ "where").text.toString // table1.city!=CITY_ID

    var handle = inStream.map(x=>{
      var IMap =(0 to x.length-1).map(i=>(Input(i),x(i)))
      var key = ""
      for(arg <- HBaseKey){
        val item =  IMap.toMap
        key +=item(arg)
      }
      val HBaseRow  = getValue(HBaseTable,key)
      if(HBaseRow != null){
        IMap ++= HBaseValue.split(delim).map(c=>{
          (HBaseTable+"."+c,Bytes.toString(HBaseRow.getValue(Bytes.toBytes("F"), Bytes.toBytes(c))))
        })
      }
      else{
        IMap ++= HBaseValue.split(delim).map(c=>{
          (HBaseTable+"."+c," ")  })
      }
      IMap
    })

    if(where != null){
      handle = handle.filter(x=>{
        getResult(where, x.toArray)
      })
    }
    result  = handle.map(x=>{
      //如果input output相同的字段完全相同，说明不需要规整数据，不做map
      val item = x.toMap
      (0 to output.length-1).map(i=>item.getOrElse(output(i),output(i))).toArray
    })
    result

  }
}