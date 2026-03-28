import { ElMessage } from 'element-plus';

/**
 * 颜色转换纯函数 - 提取到模块级别避免 hook 反模式
 */

// hex 颜色转 rgb 颜色
export const hexToRgb = (str: string): number[] => {
	let hexs = '';
	const reg = /^\#?[0-9A-Fa-f]{6}$/;
	if (!reg.test(str)) {
		ElMessage.warning('输入错误的hex');
		return [];
	}
	str = str.replace('#', '');
	hexs = str.match(/../g) || [];
	for (let i = 0; i < 3; i++) hexs[i] = parseInt(hexs[i], 16);
	return hexs;
};

// r 代表红色 | g 代表绿色 | b 代表蓝色
export const rgbToHex = (r: number, g: number, b: number): string => {
	const reg = /^\d{1,3}$/;
	if (!reg.test(String(r)) || !reg.test(String(g)) || !reg.test(String(b))) {
		ElMessage.warning('输入错误的rgb颜色值');
		return '';
	}
	const hexs = [r.toString(16), g.toString(16), b.toString(16)];
	for (let i = 0; i < 3; i++) if (hexs[i].length == 1) hexs[i] = `0${hexs[i]}`;
	return `#${hexs.join('')}`;
};

/**
 * 主题颜色处理 hook
 */
export function useChangeColor() {
	// color 颜色值字符串 | level 变浅的程度，限0-1之间
	const getDarkColor = (color: string, level: number): string => {
		const reg = /^\#?[0-9A-Fa-f]{6}$/;
		if (!reg.test(color)) {
			ElMessage.warning('输入错误的hex颜色值');
			return '';
		}
		const rgb = hexToRgb(color);
		for (let i = 0; i < 3; i++) rgb[i] = Math.floor(rgb[i] * (1 - level));
		return rgbToHex(rgb[0], rgb[1], rgb[2]);
	};

	// color 颜色值字符串 | level 加深的程度，限0-1之间
	const getLightColor = (color: string, level: number): string => {
		const reg = /^\#?[0-9A-Fa-f]{6}$/;
		if (!reg.test(color)) {
			ElMessage.warning('输入错误的hex颜色值');
			return '';
		}
		const rgb = hexToRgb(color);
		for (let i = 0; i < 3; i++) rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i]);
		return rgbToHex(rgb[0], rgb[1], rgb[2]);
	};

	return {
		hexToRgb,
		rgbToHex,
		getDarkColor,
		getLightColor,
	};
}
