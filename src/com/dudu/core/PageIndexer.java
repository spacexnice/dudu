package com.dudu.core;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import cn.edu.hfut.dmic.webcollector.model.Page;

import com.dudu.util.ConfigOPP;

public class PageIndexer {

	private IndexWriter indexer = null;
	private int total = 0;
	private Lock lock = null;
	
	public PageIndexer(Lock lock){
		this.init(lock,new StandardAnalyzer(ConfigOPP.LUCENE_VERSION));
	}
	public PageIndexer(Lock lock,Analyzer anlyzer){
		init(lock,anlyzer);
	}
	private void init(Lock lock,Analyzer analyzer){
		Directory directory;
		IndexWriterConfig config;
		try {
			this.lock = lock;
			directory = FSDirectory.open(new File(ConfigOPP.INDEX_PATH));
			config 	  = new IndexWriterConfig(ConfigOPP.LUCENE_VERSION, analyzer);
			indexer   = new IndexWriter(directory, config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 构建索引的外部接口函数
	 * @param page
	 */
	public void indexPage(Page page) {
		this.doIndex(page);
	}

	/**
	 * 实际上建立索引的函数
	 * @param page
	 */
	private void doIndex(Page page) {
		if(page.doc==null) return;
		Document doc = new Document();
		String text = page.doc.text();
		//这儿text可能为空，需要改进的地方
		doc.add(new TextField("title",page.doc.title(),Field.Store.YES));
		doc.add(new StringField("site",page.url,Field.Store.YES));
		doc.add(new TextField("contents",text,Field.Store.YES));
		try {
			lock.lock();
			indexer.addDocument(doc);
			this.increment();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}
	private int increment() {
		return ++total;
	}
	
	/*
	 * 提交索引
	 */
	public void commitAndClose(){
		if(indexer!=null){
			try {
				indexer.commit();
				indexer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public int getTotalIndexed(){
		return this.total;
	}
	
	
}
