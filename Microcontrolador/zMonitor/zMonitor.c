#include <avr/io.h>
#define F_CPU 1000000UL  // 1 MHz
#include <stdio.h>
#include <stdlib.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <avr/signal.h>
#include <inttypes.h>

//CONSTANTES PARA LA USART
//#define F_CPU 1000000UL  // 1 MHz
#define FOSC 157000//// Clock Speed
#define BAUD 9600
#define MYUBRR (FOSC/(8*BAUD))-1

// Constante de canal ADC a leer
#define CHANNEL 0

//Constante de tiempo de espera máximo para medida del cátodo (primera técnica)
#define MAX_DELAY 200

// Declaración de funciones
void USART_Init(unsigned int baud);
void USART_Transmit(unsigned char data);
unsigned char USART_Receive( void );
void ADC_init(void);
unsigned int ADC_read(unsigned char);
void enviarDatos(unsigned int sector);
unsigned char x= 0x00;
int contador=0;

int main(void){
	DDRB=0xFF;
	DDRD=0x03;
	USART_Init( MYUBRR );
	ADC_init();
	sei();
	unsigned int value;
	
	for(;;)
	{
		
		if(x==0xFF){
			value=ADC_read(CHANNEL);//Mide el canal definido por la constante
			enviarDatos(value);
			_delay_ms(200);
		}
		PORTB=x;
		
	}
}
void ADC_init(void) // Initialization of ADC
{
	ADMUX|=(1<<REFS0); // AVcc with external capacitor at AREF
	ADCSRA|=(1<<ADEN)|(1<<ADPS2)|(1<<ADPS1)|(1<<ADPS0);
	// Enable ADC and set Prescaler division factor as 128 (the fastest)
}

unsigned int ADC_read(unsigned char ch)
{
	ch= ch & 0b00000111; // channel must be b/w 0 to 7
	DDRC&=~(1<<ch);//Habilito el canal como entrada
	//PORTC|=(1<<ch); //Aseguro pull-up, es necesario?
	ADMUX |= ch; // selecting channel
	
	ADCSRA|=(1<<ADSC); // start conversion
	while(!(ADCSRA & (1<<ADIF))); // waiting for ADIF, conversion complete
	ADCSRA|=(1<<ADIF); // clearing of ADIF, it is done by writing 1 to it
	
	return (ADC);
}


void USART_Init( unsigned int baud )
{
	/* Set baud rate */
	UBRRH |= (unsigned char)(baud>>8);
	UBRRL |= (unsigned char)baud;
	/* Enable  transmitter */
	UCSRB |= (1<<TXEN)|(1<<RXEN)|(1<<RXCIE);
	/* Set frame format: 8data, 1stop bit */
	UCSRC |= (1<<URSEL)|(3<<UCSZ0);
	UCSRA|=(1<<U2X);
	sei();
}

void USART_Transmit( unsigned char data )
{
	/* Wait for empty transmit buffer */
	while ( !( UCSRA & (1<<UDRE)) )
	;
	/* Put data into buffer, sends the data */
	UDR = data;
}

unsigned int USART_Check( void )
{
	int recibio=0;
	/* Wait for data to be received */
	if( (UCSRA & (1<<RXC)) )
	{
		recibio=1;
	}

	/* Get and return received data from buffer */
	return recibio;
}

unsigned char USART_Receive( void )
{
	/* Wait for data to be received */
	while ( !(UCSRA & (1<<RXC)) )
	;
	/* Get and return received data from buffer */
	return UDR;
}


void enviarDatos(unsigned int sector){
	char mensaje[20];
	sprintf( mensaje, "(%d,%d)", contador, sector );
	for(int i=0;i<sizeof(mensaje);i++){
		unsigned char dato= mensaje[i];
		USART_Transmit( dato );
		if(dato==0x29)break;
		_delay_ms(200);
	}	
	contador++;
}


SIGNAL (SIG_UART_RECV) { // USART RX interrupt
	unsigned char h= USART_Receive();
	if( h ==0x4F )
	{
		h= USART_Receive();
		if(h==0x4B)
		{
			x=0xFF;
		}
	}
	else if(h==0x45)
	{
		h= USART_Receive();
		if(h==0x4E)
		{
			h= USART_Receive();
			if(h==0x44)
			{
				contador=0;
				x=0x00;
			}
		}
	}
}