#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/uaccess.h>
#include <linux/fs.h>
#include <linux/miscdevice.h>
#include <asm/ioctl.h>
#include <linux/delay.h>

#define DRIVER_AUTHOR	"CAUSW JH"
#define DRIVER_DESC	"driver for dotmatrix"

#define DOTM_NAME		"dotmatrix"
#define DOTM_MOUDLE_VERSION	"dotmatrix V1.0"
#define DOTM_ADDR		0x210

#define DOTM_MAGIC 0xBC
#define DOTM_SET_ALL		_IOW(DOTM_MAGIC, 0, int)
#define DOTM_SET_CLEAR		_IOW(DOTM_MAGIC, 1, int)
#define DOTM_SHIFT_LEFT		_IOW(DOTM_MAGIC, 2, int)
#define DOTM_SHIFT_RIGHT	_IOW(DOTM_MAGIC, 3, int)
#define DOTM_NAME_ENG		_IOW(DOTM_MAGIC, 4, int)
#define DOTM_NAME_KOR		_IOW(DOTM_MAGIC, 5, int)
#define MAKE_SIGN		_IOW(DOTM_MAGIC, 6, int)
#define DOTM_EXPLOSION		_IOW(DOTM_MAGIC, 7, int)

// gpio fpga interface provided
extern ssize_t iom_fpga_itf_read(unsigned int addr);
extern ssize_t iom_fpga_itf_write(unsigned int addr, unsigned int value);

// global
static int dotm_in_use = 0;
static char buf[20];
static unsigned char sign[8][10];
static int check = 0;

// dotmatrix fonts
unsigned char dotm_fontmap_decimal[10][10] = { 
        {0x3e,0x7f,0x63,0x73,0x73,0x6f,0x67,0x63,0x7f,0x3e}, // 0
        {0x0c,0x1c,0x1c,0x0c,0x0c,0x0c,0x0c,0x0c,0x0c,0x1e}, // 1
        {0x7e,0x7f,0x03,0x03,0x3f,0x7e,0x60,0x60,0x7f,0x7f}, // 2
        {0xfe,0x7f,0x03,0x03,0x7f,0x7f,0x03,0x03,0x7f,0x7e}, // 3
        {0x66,0x66,0x66,0x66,0x66,0x66,0x7f,0x7f,0x06,0x06}, // 4
        {0x7f,0x7f,0x60,0x60,0x7e,0x7f,0x03,0x03,0x7f,0x7e}, // 5
        {0x60,0x60,0x60,0x60,0x7e,0x7f,0x63,0x63,0x7f,0x3e}, // 6
        {0x7f,0x7f,0x63,0x63,0x03,0x03,0x03,0x03,0x03,0x03}, // 7
        {0x3e,0x7f,0x63,0x63,0x7f,0x7f,0x63,0x63,0x7f,0x3e}, // 8
        {0x3e,0x7f,0x63,0x63,0x7f,0x3f,0x03,0x03,0x03,0x03} // 9
};

unsigned char dotm_fontmap_name_eng[8][10] = {
	{0x7e, 0x41, 0x41, 0x41, 0x7e, 0x40, 0x40, 0x40, 0x40, 0x40},	// P
	{0x1c, 0x22, 0x41, 0x41, 0x41, 0x7f, 0x41, 0x41, 0x41, 0x41},	// A
	{0x7e, 0x41, 0x41, 0x41, 0x7e, 0x50, 0x48, 0x44, 0x42, 0x41},	// R
	{0x42, 0x44, 0x48, 0x50, 0x60, 0x50, 0x48, 0x44, 0x42, 0x41}, 	// K
	{0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x41, 0x22, 0x1c},	// J
	{0x7f, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x7f}, 	// I
	{0x41, 0x41, 0x41, 0x41, 0x7f, 0x41, 0x41, 0x41, 0x41, 0x41}, 	// H
	{0x3e, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x3e} 	// O
};

unsigned char dotm_fontmap_name_kor[3][10] = {
	{0x4a, 0x7a, 0x4b, 0x7a, 0x02, 0x7c, 0x04, 0x04, 0x04, 0x04},	//park
	{0x00, 0x00, 0x7d, 0x11, 0x29, 0x45, 0x01, 0x01, 0x00, 0x00},	//ji
	{0x00, 0x08, 0x3e, 0x08, 0x14, 0x14, 0x08, 0x08, 0x7f, 0x00}	//ho
};

unsigned char dotm_fontmap_full[10] = { 
        0x7f,0x7f,0x7f,0x7f,0x7f,0x7f,0x7f,0x7f,0x7f,0x7f
};

unsigned char dotm_fontmap_empty[10] = { 
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
};

unsigned char dotm_fontmap_explosion[7][10] = {
	{0x00, 0x00, 0x00, 0x08, 0x1c, 0x08, 0x00, 0x00 ,0x00, 0x00},
	{0x00, 0x00, 0x08, 0x1c, 0x36, 0x1c, 0x08, 0x00, 0x00, 0x00},
	{0x00, 0x08, 0x1c, 0x36, 0x63, 0x63, 0x36, 0x1c, 0x08, 0x00},
	{0x18, 0x3c, 0x3e, 0x63, 0x63, 0x63, 0x63, 0x3e, 0x3c, 0x18},
	{0x3c, 0x3e, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x3e, 0x3c},
	{0x3e, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x3e},
	{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}
};

int dotm_open(struct inode *pinode, struct file *pfile){
	int i;
	unsigned short wordvalue;

	if(dotm_in_use != 0) {
		return -EBUSY;
	}

	dotm_in_use = 1;
	
	memset(buf, 0, sizeof(buf));
	memset(sign, 0, sizeof(sign));

	// clear the dotmatrix
	for(i=0; i<10; i++){
		wordvalue = dotm_fontmap_full[i] & 0x7F;
		iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
	}

	return 0;
}

int dotm_release(struct inode *pinod, struct file *pfile){
	dotm_in_use = 0;
	return 0;
}

ssize_t dotm_write(struct file *pinode, const char * gdata, size_t len, loff_t *off_what){
	int ret, i;
	const char * tmp = NULL;
	int  num;
	unsigned short wordvalue;
	int idx=0, divide = 1;

	tmp =gdata;

	// copy userspace into kernel space buffer
	// tmp -> buf
	ret = copy_from_user(&num, tmp, len);
	if(ret <0){
		return -EFAULT;
	}
	while(1){
		buf[idx] = num / (10000000 / divide);

		num = num % (10000000 / divide);
		idx++;
		divide *= 10;
		
		if(divide == 10000000)
			break;
	}

	buf[idx] = num;

	for(i=0; i<8; i++){
		printk("buf: %d\n", buf[i]);
	}
	for(i=0; i<10; i++){
		num = buf[0];

		// number printed for dotmatrix
		wordvalue = dotm_fontmap_decimal[num][i] & 0x7F;

		// print dotmatrix
		iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
	}
	idx++;
	
	return len;
}

static long dotm_ioctl(struct file *pinode, unsigned int cmd, unsigned long data){
	int i, j;
	unsigned short wordvalue;
	int size;

	switch(cmd){
		case DOTM_SET_ALL:
			for(i=0; i<10; i++){
				wordvalue = dotm_fontmap_full[i] & 0x7F;
				iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
			}
			break;
		case DOTM_SET_CLEAR:
			for(i=0; i<10; i++){
				wordvalue = dotm_fontmap_empty[i] & 0x7F;
				iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
			}
			break;
		case DOTM_SHIFT_LEFT:
			for(i=0; i<10; i++){
				if(check < 7){
					if(check == 0){
						sign[0][i] = (sign[0][i] << 1) & 0x7e;
					}
					else{
						sign[0][i] = (sign[0][i] << 1) & 0x7e;
						sign[0][i] = (sign[0][i] | ((sign[1][i] >> (7-check) ) & 0x01 )) & 0x7f;	
					}
					wordvalue = sign[0][i];
				}
				else if(check < 14){
					if(check == 7)
						sign[1][i] = (sign[1][i] << 1) & 0x7e;
					else{
						sign[1][i] = (sign[1][i] << 1) & 0x7e;
						sign[1][i] = (sign[1][i] | ((sign[2][i] >> (14-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[1][i];	
				}
				else if(check < 21){
					if(check == 14)
						sign[2][i] = (sign[2][i] << 1) & 0x7e;
					else{
						sign[2][i] = (sign[2][i] << 1) & 0x7e;
						sign[2][i] = (sign[2][i] | ((sign[3][i] >> (21-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[2][i];
				}
				else if(check <28){
					if(check == 21)
						sign[3][i] = (sign[3][i] << 1) & 0x7e;
					else{
						sign[3][i] = (sign[3][i] << 1) & 0x7e;
						sign[3][i] = (sign[3][i] | ((sign[4][i] >> (28-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[3][i];
				}
				else if(check < 35){
					if(check == 28)
						sign[4][i] = (sign[4][i] << 1) & 0x7e;
					else{
						sign[4][i] = (sign[4][i] << 1) & 0x7e;
						sign[4][i] = (sign[4][i] | ((sign[5][i] >> (35-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[4][i];
				}
				else if(check < 42){
					if(check == 35)
						sign[5][i] = (sign[5][i] << 1) & 0x7e;
					else{
						sign[5][i] = (sign[5][i] << 1) & 0x7e;
						sign[5][i] = (sign[5][i] | ((sign[6][i] >> (42-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[5][i];
				}
				else{
					if(check == 42)
						sign[6][i] = (sign[6][i] << 1) & 0x7e;
					else{
						sign[6][i] = (sign[6][i] << 1) & 0x7e;
						sign[6][i] = (sign[6][i] | ((sign[7][i] >> (49-check) ) & 0x01 )) & 0x7f;
					}
					wordvalue = sign[6][i];
				}
		
				iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
				printk("check: %d, sign : %x\n", check,wordvalue);
			}

			check++;
			if(check ==49)
				check =0;
			break;
		case DOTM_SHIFT_RIGHT:
			for(i=0; i<10; i++){
				wordvalue = iom_fpga_itf_read((unsigned int) DOTM_ADDR+(i*2));
				wordvalue = (wordvalue >> 1) & 0x7F;
				iom_fpga_itf_write((unsigned int) DOTM_ADDR+(i*2), wordvalue);
			}
			break;
		case DOTM_NAME_ENG:
			size = sizeof(dotm_fontmap_name_eng) / sizeof(dotm_fontmap_name_eng[0]);

			for(i=0; i<size; i++){
				for(j=0; j<10; j++){
					wordvalue = dotm_fontmap_name_eng[i][j] & 0x7F;
					iom_fpga_itf_write((unsigned int) DOTM_ADDR+(j*2), wordvalue);
				}

				// 0.2 sec for 1 Character
				msleep(200);
			}
			break;
		case DOTM_NAME_KOR:
			size = sizeof(dotm_fontmap_name_kor) / sizeof(dotm_fontmap_name_eng[0]);
			for(i=0; i<size; i++){
				for(j=0; j<10; j++){
					wordvalue = dotm_fontmap_name_kor[i][j] & 0x7F;
					iom_fpga_itf_write((unsigned int) DOTM_ADDR+(j*2), wordvalue);
				}
				// 0.2 sec for 1 Character
				msleep(200);
			}
			break;
		case MAKE_SIGN:
			for(i=0; i<8; i++){
				for(j=0; j<10; j++){
					sign[i][j] = dotm_fontmap_decimal[buf[i]][j];
				}
			}
			
			break;
			
		case DOTM_EXPLOSION:
			for(i=0; i<7; i++){
				for(j=0; j<10; j++){
					wordvalue = dotm_fontmap_explosion[i][j] & 0x7F;
					iom_fpga_itf_write((unsigned int) DOTM_ADDR+(j*2), wordvalue);
				}
				msleep(50);
			}
			break;
			
	}
	return 0;	
}

static struct file_operations dotm_fops = {
	.owner	 = THIS_MODULE,
	.open	= dotm_open,
	.write	= dotm_write,
	.unlocked_ioctl = dotm_ioctl,
	.release = dotm_release,

};

static struct miscdevice dotm_driver = {
	.fops	= &dotm_fops,
	.name	= DOTM_NAME,
	.minor	= MISC_DYNAMIC_MINOR,
};

int dotm_init(void){
	misc_register(&dotm_driver);
	printk(KERN_INFO "driver: %s driver init\n", DOTM_NAME);

	return 0;
}

void dotm_exit(void){
	misc_deregister(&dotm_driver);
	printk(KERN_INFO "driver: %s driver exit\n", DOTM_NAME);
}

module_init(dotm_init);
module_exit(dotm_exit);

MODULE_AUTHOR(DRIVER_AUTHOR);
MODULE_DESCRIPTION(DRIVER_DESC);
MODULE_LICENSE("Dual BSD/GPL");
