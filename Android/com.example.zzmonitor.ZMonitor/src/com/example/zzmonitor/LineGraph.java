package com.example.zzmonitor;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;

public class LineGraph {

	private GraphicalView view;
	
	private TimeSeries dataset = new TimeSeries("Datos EEG"); 
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	
	private XYSeriesRenderer renderer = new XYSeriesRenderer(); // This will be used to customize line 1
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph
	
	public LineGraph()
	{
		// Add single dataset to multiple dataset
		mDataset.addSeries(dataset);
		
		// Customization time for line 1!
		renderer.setColor(Color.BLUE);
		renderer.setPointStyle(PointStyle.SQUARE);
		renderer.setFillPoints(true);
		
		// Enable Zoom
		mRenderer.setZoomButtonsVisible(true);
		mRenderer.setXTitle("Tiempo(ms)");
		mRenderer.setYTitle("Datos EEG");
		mRenderer.setAxesColor(Color.BLACK);
		//mRenderer.setXAxisMax(20.0);

		mRenderer.setFitLegend(true);
		mRenderer.setShowGrid(true);

		
		// Add single renderer to multiple renderer
		mRenderer.addSeriesRenderer(renderer);
	}
	
	public GraphicalView getView(Context context) 
	{
		view =  ChartFactory.getLineChartView(context, mDataset, mRenderer);
		return view;
	}
	
	public void addNewPoints(Point p)
	{
		dataset.add(p.getX(), p.getY());
	}
	public void clearChart(){
		dataset.clear();
	}
	public double getX(int index){
		return dataset.getX(index);		
	}
	public double getY(int index){
		return dataset.getY(index);		
	}
	public int getItemCount(){
		return dataset.getItemCount();
	}
	public void pop(){
		dataset.remove(0);
	}

	public boolean isEmpty() {
		
		return dataset.getItemCount()==0;
	}
	
}
